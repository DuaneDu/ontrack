import Head from "next/head";
import {pageTitle} from "@components/common/Titles";
import {Empty, Skeleton} from "antd";
import MainPage from "@components/layouts/MainPage";
import {useGraphQLClient} from "@components/providers/ConnectionContextProvider";
import {useContext, useEffect, useState} from "react";
import {gql} from "graphql-request";
import AutoVersioningAuditEntry from "@components/extension/auto-versioning/AutoVersioningAuditEntry";
import {AutoVersioningAuditContext} from "@components/extension/auto-versioning/AutoVersioningAuditContext";
import {homeBreadcrumbs} from "@components/common/Breadcrumbs";
import {AutoRefreshContextProvider} from "@components/common/AutoRefresh";

export default function AutoVersioningAuditEntryView({uuid}) {

    const auditContext = useContext(AutoVersioningAuditContext)

    const [breadcrumbs, setBreadcrumbs] = useState([])
    const [commands, setCommands] = useState([])
    useEffect(() => {
        if (auditContext) {
            // Sets the breadcrumbs
            setBreadcrumbs(homeBreadcrumbs())
        }
    }, [auditContext]);

    const client = useGraphQLClient()

    const [loading, setLoading] = useState(true)
    const [initialLoad, setInitialLoad] = useState(false)
    const [entry, setEntry] = useState({})

    const loadEntry = () => {
        if (client && uuid) {
            setLoading(true)
            client.request(
                gql`
                    query GetAVAuditDetail(
                        $uuid: String!,
                    ) {
                        autoVersioningAuditEntries(filter: {uuid: $uuid}) {
                            pageItems {
                                mostRecentState {
                                    creation {
                                        time
                                    }
                                    state
                                    data
                                }
                                duration
                                running
                                audit {
                                    creation {
                                        time
                                    }
                                    state
                                    data
                                }
                                routing
                                queue
                                order {
                                    uuid
                                    sourceProject
                                    branch {
                                        id
                                        name
                                        project {
                                            id
                                            name
                                        }
                                    }
                                    repositoryHtmlURL
                                    targetPaths
                                    targetRegex
                                    targetProperty
                                    targetPropertyRegex
                                    targetPropertyType
                                    targetVersion
                                    autoApproval
                                    autoApprovalMode
                                    upgradeBranchPattern
                                    postProcessing
                                    postProcessingConfig
                                    validationStamp
                                }
                            }
                        }
                    }
                `,
                {uuid}
            ).then(data => {
                const entries = data.autoVersioningAuditEntries.pageItems
                if (entries) {
                    setEntry(entries[0])
                }
            }).finally(() => {
                setInitialLoad(true)
                setLoading(false)
            })
        }
    }

    useEffect(() => {
        loadEntry()
    }, [client, uuid]);

    const [title, setTitle] = useState('')
    useEffect(() => {
        if (entry.order) {
            setTitle(`AV audit entry ${entry.order.uuid}`)
        }
    }, [entry]);

    return (
        <>
            <Head>
                {pageTitle(title)}
            </Head>
            <MainPage
                title={title}
                breadcrumbs={breadcrumbs}
                commands={commands}
            >
                <AutoRefreshContextProvider onRefresh={loadEntry}>
                    <Skeleton active loading={loading && !initialLoad}>
                        {
                            entry && <AutoVersioningAuditEntry entry={entry}/>
                        }
                        {
                            !entry && <Empty description="Audit entry not found"/>
                        }
                    </Skeleton>
                </AutoRefreshContextProvider>
            </MainPage>
        </>
    )
}