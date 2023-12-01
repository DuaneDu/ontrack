import BranchLink from "@components/branches/BranchLink";
import ProjectLink from "@components/projects/ProjectLink";
import HomeLink from "@components/common/HomeLink";

export function homeBreadcrumbs() {
    return [
        <HomeLink key="home"/>,
    ]
}

export function projectBreadcrumbs() {
    return [
        <HomeLink key="home"/>,
    ]
}

export function branchBreadcrumbs(branch) {
    return [
        <HomeLink key="home"/>,
        <ProjectLink project={branch.project} key="project"/>,
    ]
}

export function downToBranchBreeadcrumbs({branch}) {
    return [
        <HomeLink key="home"/>,
        <ProjectLink project={branch.project} key="project"/>,
        <BranchLink branch={branch} key="branch"/>,
    ]
}

export function buildBreadcrumbs(build) {
    return downToBranchBreeadcrumbs(build)
}