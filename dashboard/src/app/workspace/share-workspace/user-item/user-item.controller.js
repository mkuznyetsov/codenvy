/*
 *  [2015] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
'use strict';

/**
 * Controller for a permission user item.
 *
 * @author Ann Shumilova
 */
export class UserItemController {

  /**
   * Default constructor that is using resource injection
   * @ngInject for Dependency injection
   */
  constructor(codenvyPermissions) {
    this.codenvyPermissions = codenvyPermissions;
  }

  /**
   * Call user permissions removal.
   */
  removeUser() {
    //Callback is set in scope definition:
    this.callback.removePermissions(this.user);
  }

  /**
   * Returns string with user actions.
   *
   * @returns {string} string format of actions array
   */
  getUserActions() {
    //User is set in scope definition:
    return this.user.permissions.actions.join(', ');
  }
}

