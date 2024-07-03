import { login } from '../../common/apiCalls/common';
import { tasks } from '../../common/apiCalls/testData/testData';
import {
  findBatchOperation,
  openBatchOperationMenu,
  selectAll,
  selectOperation,
} from '../../common/batchOperations';
import { waitForGlobalLoading } from '../../common/loading';
import { assertMessage, dismissMenu } from '../../common/shared';
import {
  getTranslationCell,
  visitTranslations,
} from '../../common/translations';

describe('Tasks from batch operations view', () => {
  const user = 'Tasks test user';
  beforeEach(() => {
    tasks.clean({ failOnStatusCode: false });
    tasks
      .generateStandard()
      .then((r) => r.body)
      .then(({ users, projects }) => {
        login(users.find((u) => u.name === user)?.username);
        const testProject = projects.find(
          ({ name }) => name === 'Project with tasks'
        );
        visitTranslations(testProject.id);
      });
    waitForGlobalLoading();
  });

  it('creates task from bach operations', () => {
    cy.gcy('translations-row-checkbox').eq(2).click();
    selectOperation('Create task');

    cy.gcy('create-task-field-name').type('Brand new translate task');
    cy.gcy('create-task-field-languages').click();
    cy.gcy('create-task-field-languages-item').contains('English').click();
    dismissMenu();

    cy.gcy('create-task-submit').click();

    assertMessage('1 task created');

    waitForGlobalLoading();
    getTranslationCell('key 2', 'cs')
      .findDcy('translations-task-indicator')
      .should('be.visible')
      .trigger('mouseover');

    cy.gcy('task-tooltip-action-detail').click();

    cy.gcy('task-detail-keys').should('contain', 1);
    cy.gcy('task-detail-words').should('contain', 2);
  });

  it('adds keys to existing task', () => {
    selectAll();
    openBatchOperationMenu();
    findBatchOperation('Remove keys from task').should('not.exist');
    findBatchOperation('Add keys to task').click();

    cy.gcy('task-select').click();
    cy.gcy('task-select-item').contains('Translate task').click();
    cy.gcy('batch-operations-submit-button').click();

    assertMessage('Keys added to task');
    waitForGlobalLoading();
    getTranslationCell('key 0', 'en')
      .findDcy('translations-task-indicator')
      .should('be.visible')
      .trigger('mouseover');

    cy.gcy('task-tooltip-action-detail').click();

    cy.gcy('task-detail-keys').should('contain', 4);
    cy.gcy('task-detail-words').should('contain', 8);
  });

  it('removes keys from existing task', () => {
    waitForGlobalLoading();
    getTranslationCell('key 0', 'en')
      .findDcy('translations-task-indicator')
      .should('be.visible')
      .trigger('mouseover');

    cy.gcy('task-tooltip-action-translations').click();
    waitForGlobalLoading();

    selectAll();
    openBatchOperationMenu();
    findBatchOperation('Add keys to task').should('not.exist');
    findBatchOperation('Remove keys from task').click();

    cy.gcy('batch-operations-submit-button').click();

    assertMessage('Keys removed from task');

    cy.gcy('global-empty-list').should('be.visible');
  });
});
