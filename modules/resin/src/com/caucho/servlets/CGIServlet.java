/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.servlets;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.log.Log;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * CGI
 */
public class CustomCGIServlet extends GenericServlet {
  static protected final Logger log = Log.open(CustomCGIServlet.class);
  static final L10N L = new L10N(CustomCGIServlet.class);
  
  private static String REQUEST_URI = "javax.servlet.include.request_uri";
  private static String CONTEXT_PATH = "javax.servlet.include.context_path";
  private static String SERVLET_PATH = "javax.servlet.include.servlet_path";
  private static String PATH_INFO = "javax.servlet.include.path_info";
  private static String QUERY_STRING = "javax.servlet.include.query_string";

  private String _executable;
  private boolean _stderrIsException = true;
  private boolean _ignoreExitCode = false;

  /**
   * Sets an executable to run the script.
   */
  public void setExecutable(String executable)
  {
    _executable = executable;
  }

  public void setStderrIsException(boolean isException)
  {
    _stderrIsException = isException;
  }

  /**
   * If true, do not treat a non-zero exit code as an error, default false.
   */
  public void setIgnoreExitCode(boolean ignoreExitCode)
  {
    _ignoreExitCode = ignoreExitCode;
  }

  /**
   * Handle the request.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    String requestURI;
    String contextPath;
    String servletPath;
    String servletPathInfo;
    String queryString;

    requestURI = (String) req.getAttribute(REQUEST_URI);
    
    if (requestURI != null) {
      contextPath = (String) req.getAttribute(CONTEXT_PATH);
      servletPath = (String) req.getAttribute(SERVLET_PATH);
      servletPathInfo = (String) req.getAttribute(PATH_INFO);
      queryString = (String) req.getAttribute(QUERY_STRING);
    }
    else {
      requestURI = req.getRequestURI();
      contextPath = req.getContextPath();
      servletPath = req.getServletPath();
      servletPathInfo = req.getPathInfo();
      queryString = req.getQueryString();
    }

    String scriptPath;
    String pathInfo;

    if (servletPathInfo == null) {
      scriptPath = servletPath;
      pathInfo = null;
    }
    else {
      String fullPath = servletPath + servletPathInfo;
      int i = findScriptPathIndex(req, fullPath);

      if (i < 0) {
        if (log.isLoggable(Level.FINE))
          log.fine(L.l("no script path index for `{0}'", fullPath));

        res.sendError(res.SC_NOT_FOUND);

        return;
      }

      scriptPath = fullPath.substring(0, i);
      pathInfo = fullPath.substring(i);

      if ("".equals(pathInfo))
	pathInfo = null;
    }

    String realPath = req.getRealPath(scriptPath);

    Path vfsPath = Vfs.lookup(realPath);

    if (! vfsPath.canRead() || vfsPath.isDirectory()) {
      if (log.isLoggable(Level.FINE))
        log.fine(L.l("script `{0}' is unreadable", vfsPath));

      res.sendError(res.SC_NOT_FOUND);

      return;
    }

    String []env = createEnvironment(req, requestURI, contextPath,
                                     scriptPath, pathInfo, queryString);

    String []args = getArgs(realPath);

    Runtime runtime = Runtime.getRuntime();
    Process process = null;
    Alarm alarm = null;
    
    try {
      File dir = new File(Vfs.lookup(realPath).getParent().getNativePath());
			  
      if (log.isLoggable(Level.FINE)) {
        CharBuffer argsBuf = new CharBuffer();

        argsBuf.append('[');

        for (String arg : args) {
          if (argsBuf.length() > 1)
            argsBuf.append(", ");

          argsBuf.append('"');
          argsBuf.append(arg);
          argsBuf.append('"');
        }

        argsBuf.append(']');

        log.fine(L.l("exec {0} (pwd={1})", argsBuf, dir));

        if (log.isLoggable(Level.FINEST)) {
          for (String envElement : env)
            log.finest(envElement);
        }
      }

      process = runtime.exec(args, env, dir);

      PrintWriter out = res.getWriter();

      InputStream inputStream = process.getInputStream();
      InputStream errorStream = process.getErrorStream();

      TimeoutAlarm timeout;
      timeout = new TimeoutAlarm(requestURI, process, inputStream);
      alarm = new Alarm(timeout, 360 * 1000);
    
      OutputStream outputStream = process.getOutputStream();

      try {
	ServletInputStream sis = req.getInputStream();
	if (sis.available() > 0) {
	  WriteStream ws = Vfs.openWrite(outputStream);
	  try {
	    ws.writeStream(sis);
	  } finally {
	    ws.close();
	  }
	}
	outputStream.flush();
      } finally {
	outputStream.close();
      }

      ReadStream rs = Vfs.openRead(inputStream);
      boolean hasStatus = false;

      try {
	hasStatus = parseHeaders(req, res, rs);
      
	int ch;
	while ((ch = rs.read()) >= 0)
	  out.print((char) ch);
      } finally {
	try {
	  rs.close();
	} catch (Throwable e) {
	  log.log(Level.FINER, e.toString(), e);
	  
	}
	
	inputStream.close();
      }

      StringBuilder error = new StringBuilder();
      boolean hasContent = false;
      int ch;

      while (errorStream.available() > 0 && (ch = errorStream.read()) > 0) {
	error.append((char) ch);

	if (! Character.isWhitespace((char) ch))
	  hasContent = true;
      }
      errorStream.close();

      if (hasContent) {
	String errorString = error.toString();
	
	log.warning(errorString);

	if (! hasStatus && _stderrIsException)
	  throw new ServletException(errorString);
      }
    
      int exitCode = process.waitFor();

      if (exitCode != 0) {
        if (hasStatus) {
          if (log.isLoggable(Level.FINER))
            log.finer(L.l("exit code {0} (ignored, hasStatus)", exitCode));
        }
        else if (_ignoreExitCode) {
          if (log.isLoggable(Level.FINER))
            log.finer(L.l("exit code {0} (ignored)", exitCode));
        }
        else 
	  throw new ServletException(L.l("CGI execution failed.  Exit code {0}",
				         exitCode));
      }
    } catch (IOException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    } finally {
      if (alarm != null)
	alarm.dequeue();

      try {
	process.destroy();
      } catch (Throwable e) {
      }
    }
  }

  /**
   * Returns the index to the script path.
   */
  private int findScriptPathIndex(HttpServletRequest req, String fullPath)
  {
    String realPath = req.getRealPath(fullPath);
    Path path = Vfs.lookup(realPath);

    if (log.isLoggable(Level.FINER))
      log.finer(L.l("real-path is `{0}'", path));

    if (path.canRead() && ! path.isDirectory())
      return fullPath.length();

    int tail = fullPath.length();
    int head;

    while ((head = fullPath.lastIndexOf('/', tail)) >= 0) {
      String subPath = fullPath.substring(0, head);

      realPath = req.getRealPath(subPath);
      path = Vfs.lookup(realPath);

      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("trying script path {0}", path));

      if (path.canRead() && ! path.isDirectory())
	return head;

      tail = head - 1;
    }

    return -1;
  }
    
  private String []getArgs(String path)
  {
    if (_executable != null)
      return new String[] { _executable, path };
    
    ReadStream is = null;
    try {
      is = Vfs.lookup(path).openRead();

      int ch;
      if (is.read() != '#')
        return new String[] { path };
      else if (is.read() != '!')
        return new String[] { path };

      CharBuffer cb = CharBuffer.allocate();
      ArrayList<String> list = new ArrayList<String>();
      ch = is.read();

      while ((ch >= 0 && ch != '\r' && ch != '\n')) {
        for (; ch == ' ' || ch == '\t'; ch = is.read()) {
        }

        if (ch < 0 || ch == '\r' || ch == '\n') {
          if (list.size() > 0) {
            list.add(path);
            return list.toArray(new String[list.size()]);
          }
          else
            return new String[] { path };
        }

        cb.clear();
        while (ch > 0 && ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n') {
          cb.append((char) ch);
          
          ch = is.read();
        }

        list.add(cb.toString());

        for (; ch == ' ' || ch == '\t'; ch = is.read()) {
        }
      }

      if (list.size() > 0) {
        list.add(path);
        return list.toArray(new String[list.size()]);
      }
      else
        return new String[] { path };
    } catch (Exception e) {
      return new String[] { path };
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    }
  }

  private String[] createEnvironment(HttpServletRequest req,
                                     String requestURI, String contextPath,
                                     String scriptPath, String pathInfo,
                                     String queryString)
  {
    ArrayList<String> env = new ArrayList<String>();

    env.add("SERVER_SOFTWARE=Resin/" + com.caucho.Version.VERSION);
    
    env.add("SERVER_NAME=" + req.getServerName());
    //env.add("SERVER_ADDR=" + req.getServerAddr());
    env.add("SERVER_PORT=" + req.getServerPort());
    
    env.add("REMOTE_ADDR=" + req.getRemoteAddr());
    // env.add("REMOTE_PORT=" + req.getRemotePort());

    if (req.getRemoteUser() != null)
      env.add("REMOTE_USER=" + req.getRemoteUser());
    if (req.getAuthType() != null)
      env.add("AUTH_TYPE=" + req.getAuthType());
    
    env.add("GATEWAY_INTERFACE=CGI/1.1");
    env.add("SERVER_PROTOCOL=" + req.getProtocol());
    env.add("REQUEST_METHOD=" + req.getMethod());
    if (queryString != null)
      env.add("QUERY_STRING="+ queryString);
    env.add("REQUEST_URI=" + requestURI);

    // PHP needs SCRIPT_FILENAME or it reports "No input file specified."
    env.add("SCRIPT_FILENAME=" + req.getRealPath(scriptPath));

    scriptPath = contextPath + scriptPath;

    env.add("SCRIPT_NAME=" + scriptPath);

    if (pathInfo != null) {
      env.add("PATH_INFO=" + pathInfo);
      env.add("PATH_TRANSLATED=" + req.getRealPath(pathInfo));
    }

    Enumeration e = req.getHeaderNames();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String value = req.getHeader(key);

      if (key.equalsIgnoreCase("content-length"))
        env.add("CONTENT_LENGTH=" + value);
      else if (key.equalsIgnoreCase("content-type"))
        env.add("CONTENT_TYPE=" + value);
      else if (key.equalsIgnoreCase("authorization")) {
      }
      else if (key.equalsIgnoreCase("proxy-authorization")) {
      }
      else
        env.add(convertHeader(key, value));
    }

    return (String []) env.toArray(new String[env.size()]);
  }

  private String convertHeader(String key, String value)
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append("HTTP_");
    
    for (int i = 0; i < key.length(); i++) {
      char ch = key.charAt(i);
      if (ch == '-')
        cb.append('_');
      else if (ch >= 'a' && ch <= 'z')
        cb.append((char) (ch + 'A' - 'a'));
      else
        cb.append(ch);
    }

    cb.append('=');
    cb.append(value);

    return cb.close();
  }
  
  private boolean parseHeaders(HttpServletRequest req,
			       HttpServletResponse res,
			       ReadStream rs)
    throws IOException
  {
    boolean hasStatus = false;
    
    CharBuffer key = new CharBuffer();
    CharBuffer value = new CharBuffer();

    int ch;
    
    while (true) {
      key.clear();
      value.clear();

      for (ch = rs.read();
           ch >= 0 && ch != ' ' && ch != '\r' && ch != '\n' && ch != ':';
           ch = rs.read()) {
        key.append((char) ch);
      }
      
      for (;
           ch >= 0 && ch == ' ' || ch == ':';
           ch = rs.read()) {
      }

      for (;
           ch >= 0 && ch != '\r' && ch != '\n';
           ch = rs.read()) {
        value.append((char) ch);
      }

      if (ch == '\r') {
        ch = rs.read();
        if (ch != '\n')
          rs.unread();
      }

      if (key.length() == 0)
        return hasStatus;

      String keyStr = key.toString();
      String valueStr = value.toString();

      if (log.isLoggable(Level.FINEST)) {
        log.finest(L.l("{0}: {1}", keyStr, valueStr));
      }

      if (keyStr.equalsIgnoreCase("Status")) {
        int status = 0;
        int len = valueStr.length();
        int i = 0;

	hasStatus = true;
	
        for (; i < len && (ch = valueStr.charAt(i)) >= '0' && ch <= '9'; i++)
          status = 10 * status + ch - '0';
        
        for (; i < len && (ch = valueStr.charAt(i)) == ' '; i++) {
        }

	if (status < 304)
	  res.setStatus(status);
	else
	  res.sendError(status, valueStr.substring(i));
      }
      else if (keyStr.equalsIgnoreCase("Location")) {
	String uri;
	
	if (valueStr.startsWith("/"))
	  uri = req.getContextPath() + valueStr;
	else
	  uri = valueStr;

	res.setHeader("Location", res.encodeRedirectURL(uri));
      }
      else
        res.addHeader(keyStr, valueStr);
    }
  }

  class TimeoutAlarm implements AlarmListener {
    String _uri;
    Process _process;
    InputStream _is;

    TimeoutAlarm(String uri, Process process, InputStream is)
    {
      _uri = uri;
      _process = process;
      _is = is;
    }

    public void handleAlarm(Alarm alarm)
    {
      log.warning("timing out CGI process for '" + _uri + "'");
      
      try {
	_is.close();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
      
      try {
	_process.destroy();
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
