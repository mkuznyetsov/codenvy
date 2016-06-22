/* https://jira.codenvycorp.com/browse/CODENVY-667 */
var fixAcl = function(collectionName, objectType) {
  var organizationDb = db.getSiblingDB("organization");
  var collection = organizationDb.getCollection(collectionName);
  var updateNumber = 0;

  var objectIterator = collection.find();
  while (objectIterator.hasNext()) {
    var object = objectIterator.next();
    var permissions = object.permissions;

    if (permissions != null || permissions != undefined) {
      delete object.permissions;
    }

    var creator = object.creator;
    var acl = object.acl;
    if (acl == null || acl == undefined) {
      object.acl = [];
      acl = object.acl;
    }
    var modified = false;
    var isPresentPublicAclEntry=false;
    var isPresentCreatorsAclEntry=false;
    for (var i = 0; i < acl.length;) {
      var aclEntry = acl[i];

      //remove old creator's permissions
      if (aclEntry.user == creator) {
        isPresentCreatorsAclEntry = true;
        var actions = aclEntry.actions;
        if (actions.length == 5 &&
          actions.indexOf("read") > -1 &&
          actions.indexOf("search") > -1 &&
          actions.indexOf("update") > -1 &&
          actions.indexOf("delete") > -1 &&
          actions.indexOf("setPermissions") > -1) {
            //creator's permissions are correct
          } else {
            //correct creator's permissions
            acl[i] = {
              "user": creator,
              "actions": ["search", "read", "update", "delete", "setPermissions"]
            };
            modified = true;
          }
        }

        if (aclEntry.user == "*") {
          isPresentPublicAclEntry=true;
          //add public read permissoins.
          //For predefined recipes&stacks will be added public search by tomcat
          if (aclEntry.actions.length != 1 || aclEntry.actions.indexOf("read") <= -1) {
            acl[i] = {
              "user": "*",
              "actions": ["read"]
            };
            modified = true;
          }
        }
        i++;
      }

      if (!isPresentPublicAclEntry) {
        acl.push({
          "user": "*",
          "actions": ["read"]
        });
        modified = true;
      }

      if (!isPresentCreatorsAclEntry) {
        acl.push({
          "user": creator,
          "actions": ["search", "read", "update", "delete", "setPermissions"]
        });
        modified = true;
      }

      object.acl = acl;
      if (modified) {
        collection.update({
          "_id": object._id
        }, object);
        updateNumber++;
      }
    }

    print("Fixed acl for " + updateNumber + " " + collectionName + ". Total number " + collection.count());
  }

  fixAcl("recipes", "recipe");
  fixAcl("stacks", "stack");
