/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.gen;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.View;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * Represents any stateless view.
 */
@Module
abstract public class SessionView<X> extends View<X> {
  private static final L10N L = new L10N(SessionView.class);

  private SessionGenerator<X> _sessionGenerator;

  public SessionView(SessionGenerator<X> bean)
  {
    super(bean);

    _sessionGenerator = bean;
  }

  public SessionGenerator<X> getGenerator()
  {
    return _sessionGenerator;
  }

  /**
   * Introspects the APIs methods, producing a business method for each.
   */
  @Override
  public void introspect()
  {
    super.introspect();
    
    introspectImpl();
  }

  /**
   * Introspects the APIs methods, producing a business method for
   * each.
   */
  private void introspectImpl()
  {
    for (AnnotatedMethod<? super X> method
          : getGenerator().getAnnotatedMethods()) {
      introspectMethod(method);
    }
  }
  
  private void introspectMethod(AnnotatedMethod<? super X> apiMethod)
  {
    Method javaMethod = apiMethod.getJavaMember();
      
    if (javaMethod.getDeclaringClass().equals(Object.class))
      return;
    if (javaMethod.getDeclaringClass().getName().startsWith("javax.ejb."))
      return;
    if (! Modifier.isPublic(javaMethod.getModifiers()))
      return;
    if (Modifier.isFinal(javaMethod.getModifiers())
        || Modifier.isStatic(javaMethod.getModifiers()))
      return;

    if (javaMethod.getName().startsWith("ejb")) {
      throw new ConfigException(L.l("{0}: '{1}' must not start with 'ejb'.  The EJB spec reserves all methods starting with ejb.",
                                    javaMethod.getDeclaringClass(),
                                    javaMethod.getName()));
    }

    addBusinessMethod(apiMethod);
  }
  
  abstract protected void addBusinessMethod(AnnotatedMethod<? super X> method);
}