import {
  createUser,
  deleteAllEmails,
  deleteUser,
  disableEmailVerification,
  enableEmailVerification,
  getParsedEmailVerification,
  login,
} from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { assertMessage } from '../../common/shared';

describe('User profile', () => {
  const INITIAL_EMAIL = 'honza@honza.com';
  const INITIAL_PASSWORD = 'honza';
  const EMAIL_VERIFICATION_TEXT =
    'When you change your email, new e-mail will be set after its verification';
  const NEW_EMAIL = 'pavel@honza.com';

  function visit() {
    cy.visit(HOST + '/account/profile');
  }

  beforeEach(() => {
    enableEmailVerification();
    deleteUser(INITIAL_EMAIL);
    deleteUser(NEW_EMAIL);
    createUser(INITIAL_EMAIL, INITIAL_PASSWORD);
    login(INITIAL_EMAIL, INITIAL_PASSWORD);
    deleteAllEmails();
    visit();
  });

  afterEach(() => {
    enableEmailVerification();
  });

  it('email verification on user update works', () => {
    cy.get('form').findInputByName('email').clear().type(NEW_EMAIL);
    cy.contains(EMAIL_VERIFICATION_TEXT).should('be.visible');
    cy.gcy('global-form-save-button').click();
    cy.contains('E-mail waiting for verification: pavel@honza.com').should(
      'be.visible'
    );
    getParsedEmailVerification().then((v) => {
      cy.visit(v.verifyEmailLink);
      assertMessage('E-mail was verified');
      visit();
      cy.contains('E-mail waiting for verification: pavel@honza.com').should(
        'not.exist'
      );
      cy.get('form').findInputByName('email').should('have.value', NEW_EMAIL);
    });
  });

  it('works without email verification enabled', () => {
    disableEmailVerification();
    cy.reload();
    cy.get('form').findInputByName('email').clear().type(NEW_EMAIL);
    cy.contains(EMAIL_VERIFICATION_TEXT).should('not.exist');
    cy.gcy('global-form-save-button').click();
    cy.get('form').findInputByName('email').should('have.value', NEW_EMAIL);
  });

  it('will change user settings', () => {
    cy.visit(`${HOST}/account/profile`);
    cy.contains('User profile').should('be.visible');
    cy.xpath("//*[@name='name']").clear().type('New name');
    cy.xpath("//*[@name='email']").clear().type('honza@honza.com');
    cy.contains('Save').click();
    cy.contains('User data updated').should('be.visible');
    cy.reload();
    cy.contains('User profile').should('be.visible');
    cy.xpath("//*[@name='name']").should('have.value', 'New name');
    cy.xpath("//*[@name='email']").should('have.value', 'honza@honza.com');
  });

  it('will fail when user exists', () => {
    createUser(NEW_EMAIL);
    cy.visit(`${HOST}/account/profile`);
    cy.contains('User profile').should('be.visible');
    cy.xpath("//*[@name='name']").clear().type('New name');
    cy.xpath("//*[@name='email']").clear().type(NEW_EMAIL);
    cy.contains('Save').click();
    cy.contains('User name already exists.').should('be.visible');
    cy.reload();
    cy.xpath("//*[@name='email']").should('have.value', INITIAL_EMAIL);
  });

  it('changes password', () => {
    cy.visit(`${HOST}/account/profile`);
    const superNewPassword = 'super_new_password';
    cy.xpath("//*[@name='password']").clear().type(superNewPassword);
    cy.xpath("//*[@name='passwordRepeat']").clear().type(superNewPassword);
    cy.contains('Save').click();
    assertMessage('updated');
    login(INITIAL_EMAIL, superNewPassword);
    cy.reload();
    cy.contains('User profile');
  });
});
