import {createContext, useContext, useEffect, useRef, useState} from "react";
import {DashboardContext} from "@components/dashboards/DashboardContextProvider";
import {Form} from "antd";

export const DashboardWidgetCellContext = createContext({
    // Widget title
    title: "",
    setTitle: (value) => {
    },
    // Extra commands for the widget
    extra: undefined,
    setExtra: (value) => {
    },
    // Dashboard edition
    dashboardEdition: false,
    widgetEdition: false,
    widgetSaving: false,
    widgetEditionForm: undefined,
    startWidgetEdition: () => {
    },
    saveWidgetEdition: () => {
    },
    cancelWidgetEdition: () => {
    },
    deleteWidget: () => {
    },
    // Adjusting the values received from the form
    onReceivingValues: undefined,
    onReceivingValuesHandler: (fn) => {
    },
})

export default function DashboardWidgetCellContextProvider({widget, children}) {

    const [title, setTitle] = useState("")
    const [extra, setExtra] = useState()

    const dashboardContext = useContext(DashboardContext)

    const [widgetEdition, setWidgetEdition] = useState(false)
    const [widgetSaving, setWidgetSaving] = useState(false)
    const [widgetEditionForm] = Form.useForm()

    const startWidgetEdition = () => {
        setWidgetEdition(true)
    }

    const onReceivingValues = useRef(undefined);

    const onReceivingValuesHandler = (func) => {
        onReceivingValues.current = func
    }

    const saveWidgetEdition = () => {
        if (widgetEdition) {
            setWidgetSaving(true)
            // See https://ant.design/components/form#forminstance
            widgetEditionForm.validateFields()
                .then(values => {
                    // console.log("DashboardWidgetCellContext@saveWidgetEdition@values", values)
                    // console.log("DashboardWidgetCellContext@saveWidgetEdition@onReceivingValues", !!onReceivingValues.current)
                    const actualValues = onReceivingValues.current ? onReceivingValues.current(values) : values
                    // console.log("DashboardWidgetCellContext@saveWidgetEdition@actualValues", actualValues)
                    return dashboardContext.saveWidget(widget.uuid, actualValues)
                })
                .then(() => {
                    cancelWidgetEdition()
                })
                .finally(() => {
                    setWidgetSaving(false)
                })
        }
    }

    const cancelWidgetEdition = () => {
        if (widgetEdition) {
            widgetEditionForm.resetFields()
            setWidgetEdition(false)
        }
    }

    const deleteWidget = () => {
        dashboardContext.deleteWidget(widget.uuid)
    }

    const context = {
        title,
        setTitle,
        extra,
        setExtra,
        dashboardEdition: dashboardContext?.edition,
        widgetEdition,
        widgetSaving,
        widgetEditionForm,
        startWidgetEdition,
        saveWidgetEdition,
        cancelWidgetEdition,
        deleteWidget,
        onReceivingValuesHandler,
    }

    return (
        <>
            <DashboardWidgetCellContext.Provider value={context}>
                {children}
            </DashboardWidgetCellContext.Provider>
        </>
    )
}