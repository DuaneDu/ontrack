// @ts-check
const {test, expect} = require('@playwright/test');
const {ontrack} = require("@ontrack/ontrack");
const {login} = require("../login");

test('changing status', async ({page}) => {
    // Provisioning
    const project = await ontrack().createProject()
    const branch = await project.createBranch()
    const validationStamp = await branch.createValidationStamp()
    const build = await branch.createBuild()
    const run = await build.validate(validationStamp, {status: "FAILED"})
    // Login
    await login(page)
    // TODO Navigating to the branch
    // TODO Changing the validation status
    // TODO Checking the validation status has changed
})