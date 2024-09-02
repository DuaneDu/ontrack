import StandardPage from "@components/layouts/StandardPage";
import {useContext, useEffect, useState} from "react";
import {message, Space, Table} from "antd";
import {useGraphQLClient} from "@components/providers/ConnectionContextProvider";
import {gql} from "graphql-request";
import {UserContext} from "@components/providers/UserProvider";
import {Command} from "@components/common/Commands";
import {FaPencilAlt, FaPlus, FaQuestionCircle} from "react-icons/fa";
import ConfigurationDialog, {useConfigurationDialog} from "@components/configurations/ConfigurationDialog";
import InlineCommand from "@components/common/InlineCommand";
import InlineConfirmCommand from "@components/common/InlineConfirmCommand";
import {testConfig} from "@components/configurations/ConfigurationUtils";

export default function ConfigurationPage({
                                              pageTitle,
                                              configurationType,
                                              columns = [],
                                              dialogItems = [],
                                          }) {

    const user = useContext(UserContext)
    const client = useGraphQLClient()

    const [refresh, setRefresh] = useState(0)

    const [loading, setLoading] = useState(true)
    const [configurations, setConfigurations] = useState([])

    useEffect(() => {
        if (client) {
            client.request(
                gql`
                    query ConfigurationList($configurationType: String!) {
                        configurations(configurationType: $configurationType) {
                            name
                            data
                            extra
                        }
                    }
                `,
                {
                    configurationType,
                }
            ).then(data => {
                setConfigurations(data.configurations.map(it => ({
                    ...it.data,
                    name: it.name,
                    extra: it.extra,
                })))
            }).finally(() => {
                setLoading(false)
            })
        }
    }, [client, configurationType, refresh]);

    const reload = () => {
        setRefresh(refresh + 1)
    }

    const dialog = useConfigurationDialog({
        onSuccess: reload,
        dialogItems,
        configurationType,
    })

    const [messageApi, contextHolder] = message.useMessage()

    const onCreateConfig = () => {
        dialog.start({creation: true})
    }

    const onTestConfig = (config) => {
        return async () => {
            const data = {...config}
            delete data.extra
            const connectionResult = await testConfig(client, data, configurationType)
            if (connectionResult) {
                if (connectionResult.type === 'OK') {
                    messageApi.success("Connection OK")
                } else {
                    messageApi.error(connectionResult.message)
                }
            }
        }
    }

    const onUpdateConfig = (config) => {
        return () => {
            dialog.start({config})
        }
    }

    const onDeleteConfig = (config) => {
        return () => {
            client.request(
                gql`
                    mutation DeleteConfiguration($type: String!, $name: String!) {
                        deleteConfiguration(input: {
                            type: $type,
                            name: $name,
                        }) {
                            errors {
                                message
                            }
                        }
                    }
                `,
                {
                    type: configurationType,
                    name: config.name,
                }
            ).then(reload)
        }
    }

    const actionsColumn = {
        key: "actions",
        render: (_, config) =>
            <Space>
                {
                    user?.authorizations?.global?.settings &&
                    <Space>
                        <InlineCommand
                            className="ot-configuration-test"
                            title="Tests this configuration"
                            icon={<FaQuestionCircle/>}
                            onClick={onTestConfig(config)}
                        />
                        <InlineCommand
                            title="Updates this configuration"
                            icon={<FaPencilAlt/>}
                            onClick={onUpdateConfig(config)}
                        />
                        <InlineConfirmCommand
                            title="Deletes the configuration"
                            confirm="Do you really want to delete this configuration?"
                            onConfirm={onDeleteConfig(config)}
                        />
                    </Space>
                }
            </Space>,
    }

    return (
        <>
            {contextHolder}
            <StandardPage
                pageTitle={pageTitle}
                additionalCommands={
                    user?.authorizations?.global?.settings ? [
                        <Command key="create-config" icon={<FaPlus/>} action={onCreateConfig} text="Create config"/>
                    ] : []
                }
            >
                <Table
                    loading={loading}
                    dataSource={configurations}
                    columns={[...columns, actionsColumn]}
                    rowKey={(configuration) => `config-${configuration.name}`}
                    pagination={false}
                />
            </StandardPage>
            <ConfigurationDialog configurationDialog={dialog}/>
        </>
    )
}
