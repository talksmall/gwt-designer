/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.gwt.dev.javac.asm;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;

import com.google.gwt.dev.javac.MethodArgNamesLookup;
import com.google.gwt.dev.javac.Resolver;
import com.google.gwt.dev.javac.TypeParameterLookup;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolve a method given its generic signature, including return type,
 * parameter types, and exceptions thrown.
 */
public class ResolveMethodSignature extends EmptySignatureVisitor {

  private final Resolver resolver;
  private final TreeLogger logger;
  private final JAbstractMethod method;
  private final TypeParameterLookup typeParamLookup;
  private final boolean hasReturnType;
  private final CollectMethodData methodData;
  private final Type[] argTypes;
  private final String[] argNames;
  private final boolean argNamesAreReal;
  private final MethodArgNamesLookup allMethodArgs;

  private JType[] returnType = new JType[1];
  private List<JType[]> params = new ArrayList<JType[]>();
  private List<JType[]> exceptions = new ArrayList<JType[]>();
  private JTypeParameter currentParam = null;
  private ArrayList<JType[]> bounds = null;

  /**
   * Resolve a method signature.
   * 
   * @param resolver
   * @param logger
   * @param method
   * @param typeParamLookup
   * @param hasReturnType
   * @param methodData
   * @param argTypes
   * @param argNames
   * @param argNamesAreReal
   * @param allMethodArgs
   */
  public ResolveMethodSignature(Resolver resolver, TreeLogger logger,
      JAbstractMethod method, TypeParameterLookup typeParamLookup,
      boolean hasReturnType, CollectMethodData methodData, Type[] argTypes,
      String[] argNames, boolean argNamesAreReal,
      MethodArgNamesLookup allMethodArgs) {
    this.resolver = resolver;
    this.logger = logger;
    this.method = method;
    this.typeParamLookup = typeParamLookup;
    this.hasReturnType = hasReturnType;
    this.methodData = methodData;
    this.argTypes = argTypes;
    this.argNames = argNames;
    this.argNamesAreReal = argNamesAreReal;
    this.allMethodArgs = allMethodArgs;
  }

  /**
   * @return true if resolution succeeded.
   */
  public boolean finish() {
    boolean failed = false;

    finishBound();

    // Set return type
    if (hasReturnType) {
      failed |= (returnType[0] == null);
      resolver.setReturnType(method, returnType[0]);
    }

    // Create arguments
    List<CollectAnnotationData>[] argAnnotations = methodData.getArgAnnotations();
    if (argTypes.length != params.size()) {
      // TODO(jat): remove this check
      throw new IllegalStateException(
          "Arg count mismatch between method descriptor ("
              + methodData.getDesc() + ") and signature ("
              + methodData.getSignature() + ")");
    }
    String[] names = argNames;
    boolean namesAreReal = argNamesAreReal;
    if (!namesAreReal) {
      // lookup argument names in allMethodArgs
      String[] lookupArgNames = allMethodArgs.lookup(method, methodData);
      if (lookupArgNames != null) {
        names = lookupArgNames;
        namesAreReal = true;
      }
    }
    for (int i = 0; i < argTypes.length; ++i) {
      JType argType = params.get(i)[0];
      if (argType == null) {
        failed = true;
        continue;
      }
      // Try to resolve annotations, ignore any that fail.
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
      resolver.resolveAnnotations(logger, argAnnotations[i],
          declaredAnnotations);

      // JParameter adds itself to the method
      resolver.newParameter(method, argType, names[i], declaredAnnotations,
          namesAreReal);
    }

    // Handle thrown exceptions
    for (JType[] exc : exceptions) {
      if (exc[0] == null) {
        failed = true;
        continue;
      }
      resolver.addThrows(method, exc[0]);
    }
    return !failed;
  }

  @Override
  public SignatureVisitor visitArrayType() {
    assert false : "visitArrayType called on ResolveClassTypeVariables";
    return null;
  }

  @Override
  public SignatureVisitor visitClassBound() {
    JType[] bound = new JClassType[1];
    bounds.add(bound);
    return new ResolveTypeSignature(resolver, resolver.getBinaryMapper(),
        logger, bound, typeParamLookup, null);
  }

  @Override
  public SignatureVisitor visitExceptionType() {
    JType[] exc = new JType[1];
    exceptions.add(exc);
    return new ResolveTypeSignature(resolver, resolver.getBinaryMapper(),
        logger, exc, typeParamLookup, null);
  }

  @Override
  public void visitFormalTypeParameter(String name) {
    finishBound();
    currentParam = typeParamLookup.lookup(name);
    bounds = new ArrayList<JType[]>();
  }

  @Override
  public SignatureVisitor visitInterfaceBound() {
    JType[] bound = new JType[1];
    bounds.add(bound);
    return new ResolveTypeSignature(resolver, resolver.getBinaryMapper(),
        logger, bound, typeParamLookup, null);
  }

  @Override
  public SignatureVisitor visitParameterType() {
    JType[] param = new JType[1];
    params.add(param);
    return new ResolveTypeSignature(resolver, resolver.getBinaryMapper(),
        logger, param, typeParamLookup, null);
  }

  @Override
  public SignatureVisitor visitReturnType() {
    return new ResolveTypeSignature(resolver, resolver.getBinaryMapper(),
        logger, returnType, typeParamLookup, null);
  }

  private void finishBound() {
    if (currentParam != null) {
      int n = bounds.size();
      JClassType[] boundTypes = new JClassType[n];
      for (int i = 0; i < n; ++i) {
        boundTypes[i] = (JClassType) bounds.get(i)[0];
      }
      currentParam.setBounds(boundTypes);
      currentParam = null;
      // TODO(jat): remove after debugging phase
      bounds = null;
    }
  }
}
