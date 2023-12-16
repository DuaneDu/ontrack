import FormDialog, {useFormDialog} from "@components/form/FormDialog";
import {useGraphQLClient} from "@components/providers/ConnectionContextProvider";
import {gql} from "graphql-request";
import {useEffect, useState} from "react";
import {Col, Input, Row, Space, Typography} from "antd";
import SelectableWidget from "@components/dashboards/SelectableWidget";
import {FaSearch} from "react-icons/fa";

export const useWidgetSelectionDialog = ({onAddWidget}) => {
    return useFormDialog({
        onAddWidget,
    })
}

export default function WidgetSelectionDialog({widgetSelectionDialog}) {

    const client = useGraphQLClient()

    const [availableWidgets, setAvailableWidgets] = useState([])
    useEffect(() => {
        if (client) {
            client.request(
                gql`
                    query DashboardWidgets {
                        dashboardWidgets {
                            key
                            name
                            description
                            defaultConfig
                            preferredHeight
                        }
                    }
                `
            ).then(data => {
                setAvailableWidgets(data.dashboardWidgets)
            })
        }
    }, [client])

    const [token, setToken] = useState('')

    const onSearch = (e) => {
        setToken(e.target.value.toLowerCase())
    }

    const onAddWidget = (widgetDef) => {
        // Selecting the widget
        widgetSelectionDialog.onAddWidget(widgetDef)
        // Closing the dialog
        widgetSelectionDialog.setOpen(false)
    }

    return (
        <>
            <FormDialog
                dialog={widgetSelectionDialog}
                hasOk={false}
                width={800}
                height={600}
                header={
                    <Row>
                        <Col span={12}>
                            <Typography.Title level={4}>Select widget</Typography.Title>
                        </Col>
                        <Col span={12} style={{width: '100%', textAlign: 'right'}}>
                            <Input
                                prefix={<FaSearch/>}
                                placeholder="Search for widgets"
                                allowClear={true}
                                onChange={onSearch}
                                style={{
                                    width: 200,
                                }}
                            />
                        </Col>
                    </Row>
                }
            >
                <Row wrap gutter={[8, 8]}>
                    {
                        availableWidgets
                            .filter(availableWidget => {
                                return !token || availableWidget.name.toLowerCase().indexOf(token) >= 0
                            })
                            .map(availableWidget =>
                            <Col span={12} key={availableWidget.key}>
                                <SelectableWidget
                                    widgetDef={availableWidget}
                                    addWidget={onAddWidget}
                                />
                            </Col>
                        )
                    }
                </Row>
            </FormDialog>
        </>
    )
}