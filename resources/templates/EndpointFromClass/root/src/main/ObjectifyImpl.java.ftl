  private static final int DEFAULT_LIST_LIMIT = 20;

  static {
    // Typically you would register this inside an OfyServive wrapper. See: https://code.google.com/p/objectify-appengine/wiki/BestPractices
    ObjectifyService.register(${entityType}.class);
  }

  /**
   * Returns the {@link ${entityType}} with the corresponding ID.
   *
   * @param ${idName} the ID of the entity to be retrieved
   * @return the entity with the corresponding ID
   * @throws NotFoundException if there is no {@code ${entityType}} with the provided ID.
   */
  @ApiMethod(
    name = "get",
    path = "${entityName}/{${idName}}",
    httpMethod = ApiMethod.HttpMethod.GET)
  public ${entityType} get(@Named("${idName}") ${idType} ${idName}) throws NotFoundException {
    logger.info("Getting ${entityType} with ID: " + ${idName});
    ${entityType} ${entityName} = ofy().load().type(${entityType}.class).id(${idName}).now();
    if (${entityName} == null) {
      throw new NotFoundException("Could not find ${entityType} with ID: " + ${idName});
    }
    return ${entityName};
  }

  /**
   * Inserts a new {@code ${entityType}}.
   */
  @ApiMethod(
    name = "insert",
    path = "${entityName}",
    httpMethod = ApiMethod.HttpMethod.POST)
  public ${entityType} insert(${entityType} ${entityName}) {
    // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
    // You should validate that ${entityName}.${idName} has not been set. If the ID type is not supported by the
    // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
    //
    // If your client provides the ID then you should probably use PUT instead.
    ofy().save().entity(${entityName}).now();
<#if idGetterName??>
    logger.info("Created ${entityType} with ID: " + ${entityName}.${idGetterName}());
<#else>
    logger.info("Created ${entityType}.");
</#if>

    return ofy().load().entity(${entityName}).now();
  }

  /**
   * Updates an existing {@code ${entityType}}.
   *
   * @param ${idName} the ID of the entity to be updated
   * @param ${entityName} the desired state of the entity
   * @return the updated version of the entity
   * @throws NotFoundException if the {@code ${idName}} does not correspond to an existing
   *  {@code ${entityType}}
   */
  @ApiMethod(
    name = "update",
    path = "${entityName}/{${idName}}",
    httpMethod = ApiMethod.HttpMethod.PUT)
  public ${entityType} update(@Named("${idName}") ${idType} ${idName}, ${entityType} ${entityName}) throws NotFoundException {
    // TODO: You should validate your ID parameter against your resource's ID here.
    checkExists(${idName});
    ofy().save().entity(${entityName}).now();
    logger.info("Updated ${entityType}: " + ${entityName});
    return ofy().load().entity(${entityName}).now();
  }

  /**
   * Deletes the specified {@code ${entityType}}.
   *
   * @param ${idName} the ID of the entity to delete
   * @throws NotFoundException if the {@code ${idName}} does not correspond to an existing
   *  {@code ${entityType}}
   */
  @ApiMethod(
    name = "remove",
    path = "${entityName}/{${idName}}",
    httpMethod = ApiMethod.HttpMethod.DELETE)
  public void remove(@Named("${idName}") ${idType} ${idName}) throws NotFoundException {
    checkExists(${idName});
    ofy().delete().type(${entityType}.class).id(${idName}).now();
    logger.info("Deleted ${entityType} with ID: " + ${idName});
  }

  /**
   * List all entities.
   *
   * @param cursor used for pagination to determine which page to return
   * @param limit the maximum number of entries to return
   * @return a response that encapsulates the result list and the next page token/cursor
   */
  @ApiMethod(
    name = "list",
    path = "${entityName}",
    httpMethod = ApiMethod.HttpMethod.GET)
  public CollectionResponse<${entityType}> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
    limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
    Query<${entityType}> query = ofy().load().type(${entityType}.class).limit(limit);
    if (cursor != null) {
      query = query.startAt(Cursor.fromWebSafeString(cursor));
    }
    QueryResultIterator<${entityType}> queryIterator = query.iterator();
    List<${entityType}> ${entityName}List = new ArrayList<${entityType}>(limit);
    while (queryIterator.hasNext()) {
      ${entityName}List.add(queryIterator.next());
    }
    return CollectionResponse.<${entityType}>builder().setItems(${entityName}List).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
  }

  private void checkExists(${idType} ${idName}) throws NotFoundException {
    try {
      ofy().load().type(${entityType}.class).id(${idName}).safe();
    } catch (com.googlecode.objectify.NotFoundException e) {
      throw new NotFoundException("Could not find ${entityType} with ID: " + ${idName});
    }
  }