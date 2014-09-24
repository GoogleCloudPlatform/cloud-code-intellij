  /**
   * This method gets the <code>${entityType}</code> object associated with the specified <code>id</code>.
   * @param id The id of the object to be returned.
   * @return The <code>${entityType}</code> associated with <code>id</code>.
   */
  @ApiMethod(name = "get${entityType}")
  public ${entityType} get${entityType}(@Named("id") Long id) {
    // TODO: Implement this function
    logger.info("Calling get${entityType} method");
    return null;
  }

  /**
   * This inserts a new <code>${entityType}</code> object.
   * @param ${entityName} The object to be added.
   * @return The object to be added.
   */
  @ApiMethod(name = "insert${entityType}")
  public ${entityType} insert${entityType}(${entityType} ${entityName}) {
    // TODO: Implement this function
    logger.info("Calling insert${entityType} method");
    return ${entityName};
  }
