const {test} = require("@playwright/test");
const {ontrack} = require("@ontrack/ontrack");
const {login} = require("../login");
const {BuildPage} = require("../builds/build");

test('build page', async ({page}) => {
    // Provisioning
    const project = await ontrack().createProject()
    const branch = await project.createBranch()
    const build = await branch.createBuild()
    // Login
    await login(page)
    // Navigating to the build
    const buildPage = new BuildPage(page, build)
    await buildPage.goTo()
})