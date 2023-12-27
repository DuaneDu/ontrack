import {useEffect, useState} from "react";
import {useGraphQLClient} from "@components/providers/ConnectionContextProvider";
import {gql} from "graphql-request";
import ChangeLogBuild from "@components/extension/scm/ChangeLogBuild";
import Head from "next/head";
import {buildKnownName, title} from "@components/common/Titles";
import MainPage from "@components/layouts/MainPage";
import LoadingContainer from "@components/common/LoadingContainer";
import {downToBranchBreadcrumbs} from "@components/common/Breadcrumbs";
import {CloseCommand} from "@components/common/Commands";
import {branchUri} from "@components/common/Links";
import GridTable from "@components/grid/GridTable";
import GridTableContextProvider, {GridTableContext} from "@components/grid/GridTableContext";
import GitChangeLogCommits from "@components/extension/git/GitChangeLogCommits";
import ChangeLogIssues from "@components/extension/issues/ChangeLogIssues";
import ChangeLogLinks from "@components/extension/scm/ChangeLogLinks";

export default function GitChangeLogView({from, to}) {

    const client = useGraphQLClient()

    const [loading, setLoading] = useState(true)
    const [buildFrom, setBuildFrom] = useState({})
    const [buildTo, setBuildTo] = useState({})

    const [changeLogUuid, setChangeLogUuid] = useState('')
    const [diffLink, setDiffLink] = useState('')

    const gqlBuildData = gql`
        fragment BuildData on Build {
            id
            name
            creation {
                time
            }
            branch {
                id
                name
                project {
                    id
                    name
                }
            }
            promotionRuns(lastPerLevel: true) {
                id
                creation {
                    time
                }
                promotionLevel {
                    id
                    name
                    description
                    image
                    _image
                }
            }
            releaseProperty {
                value
            }
        }
    `

    useEffect(() => {
        if (client && from && to) {
            setLoading(true)
            client.request(
                gql`
                    query ChangeLog($from: Int!, $to: Int!) {
                        gitChangeLog(from: $from, to: $to) {
                            buildFrom {
                                ...BuildData
                            }
                            buildTo {
                                ...BuildData
                            }
                            uuid
                            diffLink
                        }
                    }
                    ${gqlBuildData}
                `,
                {from, to}
            ).then(data => {
                const changeLog = data.gitChangeLog
                setChangeLogUuid(changeLog.uuid)
                setDiffLink(changeLog.diffLink)
                setBuildFrom(changeLog.buildFrom)
                setBuildTo(changeLog.buildTo)
            }).finally(() => {
                setLoading(false)
            })
        }
    }, [client, from, to]);


    const defaultLayout = [
        {i: "from", x: 0, y: 0, w: 6, h: 5},
        {i: "to", x: 6, y: 0, w: 6, h: 5},
        {i: "links", x: 0, y: 5, w: 12, h: 7},
        {i: "commits", x: 0, y: 12, w: 12, h: 10},
        {i: "issues", x: 0, y: 22, w: 12, h: 10},
    ]

    const items = [
        {
            id: "from",
            content: <ChangeLogBuild id="from" title={`From ${buildKnownName(buildFrom)}`} build={buildFrom}/>,
        },
        {
            id: "to",
            content: <ChangeLogBuild id="to" title={`To ${buildKnownName(buildTo)}`} build={buildTo}/>,
        },
        {
            id: "links",
            content: <ChangeLogLinks id="links" changeLogUuid={changeLogUuid}/>
        },
        {
            id: "commits",
            content: <GitChangeLogCommits id="commits" changeLogUuid={changeLogUuid} diffLink={diffLink}/>
        },
        {
            id: "issues",
            content: <ChangeLogIssues id="issues" changeLogUuid={changeLogUuid}/>
        },
    ]

    return (
        <>
            <Head>
                {title(`Change log | From ${buildKnownName(buildFrom)} to ${buildKnownName(buildTo)}`)}
            </Head>
            <MainPage
                title={
                    `Change log from ${buildKnownName(buildFrom)} to ${buildKnownName(buildTo)}`
                }
                breadcrumbs={buildFrom.branch ? downToBranchBreadcrumbs(buildFrom) : []}
                commands={[
                    <CloseCommand
                        key="close"
                        href={buildFrom.from ? branchUri(buildFrom.branch) : ''}
                    />,
                ]}
            >
                <GridTableContextProvider isExpandable={false} isDraggable={false}>
                    <LoadingContainer loading={loading} tip="Loading change log">
                        <GridTable
                            rowHeight={30}
                            layout={defaultLayout}
                            items={items}
                            isResizable={false}
                            isDraggable={false}
                        />
                    </LoadingContainer>
                </GridTableContextProvider>
            </MainPage>
        </>
    )
}