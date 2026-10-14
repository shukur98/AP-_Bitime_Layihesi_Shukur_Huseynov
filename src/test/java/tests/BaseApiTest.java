package tests;

import helper.AuthHelper;

/**
 * Auth is a required setup condition for authenticated tests.
 * Missing/wrong credentials are configuration ERRORS, never false PASS or SKIPPED.
 * Only tests requiring optional fixtures may be skipped using JUnit assumptions.
 */
public abstract class BaseApiTest {
    protected String user1Token() { return AuthHelper.token(1); }
    protected String user2Token() { return AuthHelper.token(2); }
}
