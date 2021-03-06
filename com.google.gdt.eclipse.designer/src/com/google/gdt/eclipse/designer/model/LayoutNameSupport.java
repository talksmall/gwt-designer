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
package com.google.gdt.eclipse.designer.model;

import com.google.common.collect.Maps;
import com.google.gdt.eclipse.designer.preferences.IPreferenceConstants;

import org.eclipse.wb.core.model.JavaInfo;

import org.eclipse.jface.preference.IPreferenceStore;

import java.util.Map;

/**
 * Support for managing name of <code>Layout</code>, so that it corresponds to the name of its
 * parent <code>Container</code>.
 * 
 * @author sablin_aa
 * @coverage gwt.model
 */
public abstract class LayoutNameSupport<T extends JavaInfo>
    extends
      org.eclipse.wb.internal.core.model.layout.LayoutNameSupport<T> {
  public static final String[] TEMPLATES = new String[]{
      "${layoutAcronym}_${containerName}",
      "${layoutAcronym}${containerName-cap}",
      "${containerName}${layoutClassName}",
      "${defaultName}"};

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public LayoutNameSupport(T layout) {
    super(layout);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utilities
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected String getTemplate() {
    IPreferenceStore preferences = m_childInfo.getDescription().getToolkit().getPreferences();
    String template = preferences.getString(IPreferenceConstants.P_LAYOUT_NAME_TEMPLATE);
    if (!isValidTemplate(TEMPLATES, template)) {
      template = getTemplateForDefault();
    }
    return template;
  }

  @Override
  protected Map<String, String> getValueMap() {
    // prepare variables
    Map<String, String> valueMap = Maps.newTreeMap();
    {
      valueMap.put("layoutAcronym", getAcronym());
      valueMap.put("layoutClassName", getClassName());
      valueMap.put("containerName", getParentName());
      valueMap.put("containerName-cap", getParentNameCap());
    }
    return valueMap;
  }
}