import {useContext, useState} from "react";
import {gql} from "graphql-request";
import ProjectBox from "@components/projects/ProjectBox";
import {Space} from "antd";
import SimpleWidget from "@components/dashboards/widgets/SimpleWidget";
import {FaPlus} from "react-icons/fa";
import WidgetCommand from "@components/dashboards/commands/WidgetCommand";
import NewProjectDialog, {useNewProjectDialog} from "@components/projects/NewProjectDialog";
import {UserContext} from "@components/providers/UserProvider";
import LastActiveProjectsWidgetForm from "@components/dashboards/widgets/home/LastActiveProjectsWidgetForm";
import RowTag from "@components/common/RowTag";

export default function LastActiveProjectsWidget({count}) {

    const user = useContext(UserContext)

    const [projects, setProjects] = useState([])
    const [projectsRefreshCount, setProjectsRefreshCount] = useState(0)

    const newProjectDialog = useNewProjectDialog({
        onSuccess: () => {
            setProjectsRefreshCount(projectsRefreshCount + 1)
        }
    })

    const createProject = () => {
        newProjectDialog.start()
    }

    const getCommands = (/*projects*/) => {
        return [
            <WidgetCommand
                key="project-create"
                condition={user.authorizations.project?.create}
                title="Create project"
                icon={<FaPlus/>}
                onAction={createProject}
            />
        ]
    }

    return (
        <>
            <NewProjectDialog newProjectDialog={newProjectDialog}/>
            <SimpleWidget
                title={`Last ${count} active projects`}
                query={
                    gql`
                        query LastActiveProjects($count: Int! = 10) {
                            lastActiveProjects(count: $count) {
                                id
                                name
                                favourite
                            }
                        }
                    `
                }
                queryDeps={[user, count, projectsRefreshCount]}
                variables={{count}}
                setData={data => setProjects(data.lastActiveProjects)}
                getCommands={projects => getCommands(projects)}
                form={<LastActiveProjectsWidgetForm count={count}/>}
            >
                <Space direction="horizontal" size={16} wrap>
                    {
                        projects.map(project => <RowTag>
                                <ProjectBox key={project.id} project={project}/>
                            </RowTag>
                        )
                    }
                </Space>
            </SimpleWidget>
        </>
    )
}