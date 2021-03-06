/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.boot;

import com.caucho.bam.Query;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.bam.broker.Broker;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.util.logging.*;

/**
 * BAM service managing the watchdog
 */
class WatchdogService extends SimpleActor
{
  private static final L10N L = new L10N(WatchdogService.class);
  private static final Logger log
    = Logger.getLogger(WatchdogService.class.getName());

  private final WatchdogManager _manager;
  private final String _address;

  WatchdogService(WatchdogManager manager,
                  String address,
                  Broker broker)
  {
    _manager = manager;
    _address = address;
    
    setBroker(broker);
  }

  /**
   * Returns the server id of the watchdog.
   */
  @Override
  public String getAddress()
  {
    return _address;
  }

  /**
   * Start queries
   */
  @Query
  public boolean watchdogStart(long id, String to, String from,
                               WatchdogStartQuery start)
  {
    String []argv = start.getArgv();

    try {
      _manager.startServer(argv);

      String msg = L.l("{0}: started server", this);
    
      getBroker().queryResult(id, from, to,
                                    new ResultStatus(true, msg));
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      String msg;

      if (e instanceof ConfigException)
        msg = e.getMessage();
      else
        msg = L.l("{0}: start server failed because of exception\n  {1}'",
                  this, e.toString());
    
      getBroker().queryResult(id, from, to,
                                    new ResultStatus(false, msg));
    }
    
    return true;
  }

  /**
   * Status queries
   */
  @Query
  public boolean watchdogStatus(long id, String to, String from,
                                WatchdogStatusQuery status)
  {
    try {
      String result = _manager.status();
    
      getBroker().queryResult(id, from, to,
                              new ResultStatus(true, result));
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      String msg = L.l("{0}: status failed because of exception\n{1}'",
                       this, e.toString());
    
      getBroker().queryResult(id, from, to,
                              new ResultStatus(false, msg));
    }
    
    return true;
  }

  /**
   * Handles stop queries
   */
  @Query
  public boolean watchdogStop(long id, String to, String from,
                              WatchdogStopQuery stop)
  {
    String serverId = stop.getServerId();

    try {
      _manager.stopServer(serverId);

      String msg = L.l("{0}: stopped server='{1}'", this, serverId);
    
      getBroker().queryResult(id, from, to,
                                    new ResultStatus(true, msg));
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      String msg = L.l("{0}: stop server='{1}' failed because of exception\n{2}'",
                       this, serverId, e.toString());
    
      getBroker().queryResult(id, from, to,
                                    new ResultStatus(false, msg));
    }
    
    if (_manager.isEmpty()) {
      new Thread(new Shutdown()).start();
    }
    
    return true;
  }

  /**
   * Handles stop queries
   */
  @Query
  public boolean watchdogRestart(long id, String to, String from,
                                 WatchdogRestartQuery restart)
  {
    String serverId = restart.getServerId();
    String []argv = restart.getArgv();

    try {
      _manager.restartServer(serverId, argv);

      String msg = L.l("{0}: restarted server='{1}'", this, serverId);
    
      getBroker().queryResult(id, from, to,
                                    new ResultStatus(true, msg));
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      String msg = L.l("{0}: restart server='{1}' failed because of exception\n{2}'",
                       this, serverId, e.toString());
    
      getBroker().queryResult(id, from, to,
                                    new ResultStatus(false, msg));
    }
    
    if (_manager.isEmpty()) {
      new Thread(new Shutdown()).start();
    }
    
    return true;
  }

  /**
   * Handles kill queries
   */
  @Query
  public boolean watchdogKill(long id, String to, String from,
                              WatchdogKillQuery kill)
  {
    String serverId = kill.getServerId();

    try {
      _manager.killServer(serverId);

      String msg = L.l("{0}: killed server='{1}'", this, serverId);
    
      getBroker().queryResult(id, from, to,
                                    new ResultStatus(true, msg));
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      String msg = L.l("{0}: kill server='{1}' failed because of exception\n{2}'",
                       this, serverId, e.toString());
    
      getBroker().queryResult(id, from, to,
                                    new ResultStatus(false, msg));
    }
    
    if (_manager.isEmpty()) {
      new Thread(new Shutdown()).start();
    }
    
    return true;
  }

  /**
   * Handles shutdown queries
   */
  @Query
  public boolean watchdogShutdown(long id, String to, String from,
                                  WatchdogShutdownQuery shutdown)
  {
    try {
      log.info(this + " shutdown from " + from);

      String msg = L.l("{0}: shutdown", this);
    
      new Thread(new Shutdown()).start();
      
      getBroker().queryResult(id, from, to,
                                    new ResultStatus(true, msg));
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      String msg = L.l("{0}: shutdown failed because of exception\n{2}'",
                       this, e.toString());
    
      getBroker().queryResult(id, from, to,
                                    new ResultStatus(false, msg));
    }
    
    return true;
  }

  static class Shutdown implements Runnable {
    public void run()
    {
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
      }

      System.exit(0);
    }
  }
}
