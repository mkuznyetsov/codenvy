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
 * Controller for the factories.
 * @author Florent Benoit
 * @author Oleksii Orel
 */
export class ListFactoriesCtrl {

  /**
   * Default constructor that is using resource injection
   * @ngInject for Dependency injection
   */
  constructor($mdDialog, codenvyAPI, cheNotification) {
    this.$mdDialog = $mdDialog;
    this.codenvyAPI = codenvyAPI;
    this.cheNotification = cheNotification;

    this.menuOptions = [
      {
        title: 'Sort by views number',
        onclick: () => {
          this.factoriesOrderBy = this.factoriesOrderBy === 'views' ? '-' + 'views' : 'views';
        }
      },
      {
        title: 'Sort by creation date',
        onclick: () => {
          this.factoriesOrderBy = this.factoriesOrderBy === 'originFactory.creator.created' ? '-' + 'originFactory.creator.created' : 'originFactory.creator.created';
        }
      },
      {
        title: 'Delete all selected factories',
        onclick: () => {
          this.deleteSelectedFactories();
        }
      }
    ];

    this.hasNextPage = true;

    this.maxItems = 15;
    this.skipCount = 0;

    this.factoriesOrderBy = '';

    this.factoriesFilter = {
      originFactory: {
        name: ''
      }
    };

    this.factoriesSelectedStatus = {};

    this.isLoading = true;

    this.factories = codenvyAPI.getFactory().getFactories();

    // fetch factories when initializing
    let promise = codenvyAPI.getFactory().fetchFactories(this.maxItems, this.skipCount);

    promise.then(() => {
        this.isLoading = false;
      },
      (error) => {
        this.isLoading = false;
        if (error.status !== 304) {
          cheNotification.showError(error.data.message ? error.data.message : 'Update information failed.');
        }
      });
  }

  /**
   * Load more factories, coll this function on list scroll down
   */
  loadNextPage() {
    this.skipCount = this.factories.length;
    this.isLoading = true;

    let promise = this.codenvyAPI.getFactory().fetchFactories(this.maxItems, this.skipCount);

    promise.then(() => {
        this.factories = this.codenvyAPI.getFactory().getFactories();
        this.isLoading = false;
      },
      (error) => {
        this.isLoading = false;
        if (error.status !== 304) {
          this.cheNotification.showError(error.data.message ? error.data.message : 'Update information failed.');
        }
      });
  }


  /**
   * Delete all selected factories
   */
  deleteSelectedFactories() {
    let factoriesSelectedStatusKeys = Object.keys(this.factoriesSelectedStatus);
    let checkedFactoriesKeys = [];
    if (factoriesSelectedStatusKeys.length) {
      let ctrl = this;
      factoriesSelectedStatusKeys.forEach((key) => {
        if (ctrl.factoriesSelectedStatus[key] === true) {
          checkedFactoriesKeys.push(key);
        }
      });
      let queueLenth = checkedFactoriesKeys.length;
      if (queueLenth) {
        let confirmTitle = 'Would you like to delete ';
        if (queueLenth > 1) {
          confirmTitle += 'these ' + queueLenth + ' factories?';
        } else {
          confirmTitle += 'this selected factory?';
        }
        let confirm = this.$mdDialog.confirm()
          .title(confirmTitle)
          .content('Please confirm for the removal.')
          .ariaLabel('Remove selected factories')
          .ok('Delete!')
          .cancel('Cancel')
          .clickOutsideToClose(true);
        this.$mdDialog.show(confirm).then(() => {
          let isError = false;
          checkedFactoriesKeys.forEach((factoryId) => {
            this.factoriesSelectedStatus[factoryId] = false;
            // remove it !
            let promise = this.codenvyAPI.getFactory().deleteFactoryById(factoryId);
            promise.then(() => {
              queueLenth--;
              if (!queueLenth) {
                if (isError) {
                  this.cheNotification.showError('Delete failed.');
                } else {
                  this.cheNotification.showInfo('Has been successfully removed.');
                }
              }
            }, () => {
              queueLenth--;
              if (!queueLenth) {
                this.cheNotification.showError('Delete failed.');
              }
            });
          });
        });
      } else {
        this.cheNotification.showError('No selected factories.');
      }
    } else {
      this.cheNotification.showError('No selected factories.');
    }
  }
}

