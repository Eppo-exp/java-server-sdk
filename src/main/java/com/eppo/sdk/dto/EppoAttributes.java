package com.eppo.sdk.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Subject Attributes Class
 */
public class EppoAttributes extends HashMap<String, EppoValue> {

  public EppoAttributes() {
    super();
  }

  public EppoAttributes(Map<String, EppoValue> initialValues) {
    super(initialValues);
  }
}
