/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.webbeans.component;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.bytecode.*;
import com.caucho.webbeans.inject.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.context.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for the xml web bean component.
 */
public class ClassComponent extends ComponentImpl {
  private static final L10N L = new L10N(ClassComponent.class);

  private static final Object []NULL_ARGS = new Object[0];
  
  private Class _cl;
  private boolean _isBound;

  private Constructor _ctor;
  private ComponentImpl []_ctorArgs;

  private ScopeAdapter _scopeAdapter;

  public ClassComponent(WbWebBeans webbeans)
  {
    super(webbeans);
  }

  public void setInstanceClass(Class cl)
  {
    _cl = cl;
  }

  public Class getInstanceClass()
  {
    return _cl;
  }

  public void init()
  {
    introspect();

    super.init();
  }

  /**
   * Called for implicit introspection.
   */
  public void introspect()
  {
    Class cl = getInstanceClass();
    Class scopeClass = null;

    if (getType() == null) {
      for (Annotation ann : cl.getDeclaredAnnotations()) {
	if (ann.annotationType().isAnnotationPresent(ComponentType.class)) {
	  if (getType() != null)
	    throw new ConfigException(L.l("{0}: component type annotation @{1} conflicts with @{2}.  WebBeans components may only have a single @ComponentType.",
					  cl.getName(),
					  getType().getType().getName(),
					  ann.annotationType().getName()));
	
	  setType(_webbeans.createComponentType(ann.annotationType()));
	}
      }
    }

    if (getType() == null)
      setType(_webbeans.createComponentType(Component.class));

    if (getScope() == null) {
      for (Annotation ann : cl.getDeclaredAnnotations()) {
	if (ann.annotationType().isAnnotationPresent(ScopeType.class)) {
	  if (scopeClass != null)
	    throw new ConfigException(L.l("{0}: @ScopeType annotation @{1} conflicts with @{2}.  WebBeans components may only have a single @ScopeType.",
					  cl.getName(),
					  scopeClass.getName(),
					  ann.annotationType().getName()));

	  scopeClass = ann.annotationType();
	  setScope(_webbeans.getScopeContext(scopeClass));
	}
      }
    }

    if (getName() == null) {
      String name = cl.getSimpleName();

      name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
	
      setName(name);
    }

    introspectProduces();
    introspectConstructor();

    if (getBindingList().size() == 0)
      introspectBindings();
  }

  /**
   * Introspects the methods for any @Produces
   */
  private void introspectProduces()
  {
    if (_cl == null)
      return;
    
    for (Method method : _cl.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers()))
	continue;

      if (! method.isAnnotationPresent(Produces.class))
	continue;

      ProducesComponent comp = new ProducesComponent(_webbeans, this, method);

      _webbeans.addWbComponent(comp);
      
      comp.init();
    }
  }

  /**
   * Introspects the constructor
   */
  private void introspectConstructor()
  {
    try {
      Constructor best = null;
      Constructor second = null;

      for (Constructor ctor : _cl.getDeclaredConstructors()) {
	if (best == null) {
	  best = ctor;
	}
	else if (hasBindingAnnotation(ctor)) {
	  if (best != null && hasBindingAnnotation(best))
	    throw new ConfigException(L.l("WebBean {0} has two constructors with binding annotations.",
					  ctor.getDeclaringClass().getName()));
	  best = ctor;
	  second = null;
	}
	else {
	  second = ctor;
	}
      }

      _ctor = best;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Creates a new instance of the component.
   */
  public Object get(DependentScope scope)
  {
    try {
      Object value;
      boolean isNew = false;

      if (scope == null || _scope == null || scope.canInject(_scope)) {
	if (_scope != null) {
	  value = _scope.get(this);
	  
	  if (value != null)
	    return value;
	}
	else if (scope != null) {
	  value = scope.get(this);
	  
	  if (value != null)
	    return value;
	}
      
	value = createNew();
	
	if (_scope != null) {
	  _scope.put(this, value);
	  scope = new DependentScope(this, value, _scope);
	}
	else
	  scope.put(this, value);
	
	init(value, scope);
      }
      else {
	if (scope != null) {
	  value = scope.get(this);
	  
	  if (value != null)
	    return value;
	}
      
	if (_scopeAdapter == null)
	  _scopeAdapter = ScopeAdapter.create(getInstanceClass());

	value = _scopeAdapter.wrap(this);

	scope.put(this, value);
      }

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected Object createNew()
  {
    try {
      if (! _isBound)
	bind();
      
      Object []args;
      if (_ctorArgs.length > 0) {
	args = new Object[_ctorArgs.length];

	for (int i = 0; i < args.length; i++)
	  args[i] = _ctorArgs[i].create();
      }
      else
	args = NULL_ARGS;
      
      return _ctor.newInstance(args);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Binds parameters
   */
  public void bind()
  {
    synchronized (this) {
      if (_isBound)
	return;

      ArrayList<Inject> injectList = new ArrayList<Inject>();
      InjectIntrospector.introspectInject(injectList, _cl);
      InjectIntrospector.introspectInit(injectList, _cl);
      
      // InjectIntrospector.introspectInit(injectList, _cl);

      _injectProgram = new Inject[injectList.size()];
      injectList.toArray(_injectProgram);
      
      ArrayList<Inject> destroyList = new ArrayList<Inject>();
      InjectIntrospector.introspectDestroy(destroyList, _cl);
      _destroyProgram = new Inject[destroyList.size()];
      destroyList.toArray(_destroyProgram);

      String loc = _ctor.getDeclaringClass().getName() + "(): ";
      Class []param = _ctor.getParameterTypes();
      Annotation [][]paramAnn = _ctor.getParameterAnnotations();

      _ctorArgs = new ComponentImpl[param.length];

      for (int i = 0; i < param.length; i++) {
	_ctorArgs[i] = _webbeans.bindParameter(loc, param[i], paramAnn[i]);
      }

      _isBound = true;
    }
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectBindings()
  {
    ArrayList<WbBinding> bindings = new ArrayList<WbBinding>();
    
    for (Annotation ann : getInstanceClass().getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	bindings.add(new WbBinding(ann));

      if (ann instanceof Named)
	setName(((Named) ann).value());
    }

    if (bindings.size() > 0)
      setBindingList(bindings);
  }

  private boolean hasBindingAnnotation(Constructor ctor)
  {
    if (ctor.isAnnotationPresent(In.class))
      return true;

    Annotation [][]paramAnn = ctor.getParameterAnnotations();

    for (Annotation []annotations : paramAnn) {
      for (Annotation ann : annotations) {
	if (ann.annotationType().isAnnotationPresent(BindingType.class))
	  return true;
      }
    }

    return false;
  }
}
