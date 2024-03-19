import {useContext} from "react";
import {DashboardWidgetCellContext} from "@components/dashboards/DashboardWidgetCellContextProvider";
import {Form} from "antd";
import SelectProjectBranchPromotionLevel from "@components/promotionLevels/SelectProjectBranchPromotionLevel";
import SelectChartPeriod from "@components/charts/SelectChartPeriod";
import SelectChartInterval from "@components/charts/SelectChartInterval";

export default function E2EChartWidgetForm({
                                               project,
                                               branch,
                                               promotionLevel,
                                               targetProject,
                                               targetBranch,
                                               targetPromotionLevel,
                                               interval,
                                               period
                                           }) {

    const {widgetEditionForm} = useContext(DashboardWidgetCellContext)

    const {onReceivingValuesHandler} = useContext(DashboardWidgetCellContext)

    const onFormValues = (values) => {
        // Flattening
        return {
            interval: values.interval,
            period: values.period,
            project: values.promotionLevel.project,
            branch: values.promotionLevel.branch,
            promotionLevel: values.promotionLevel.promotionLevel,
            targetProject: values.targetPromotionLevel.project,
            targetBranch: values.targetPromotionLevel.branch,
            targetPromotionLevel: values.targetPromotionLevel.promotionLevel,
        }
    }

    onReceivingValuesHandler(onFormValues)

    return (
        <>
            <Form
                layout="vertical"
                form={widgetEditionForm}
            >
                <Form.Item
                    name="promotionLevel"
                    label="From promotion level..."
                    initialValue={{project, branch, promotionLevel}}
                >
                    <SelectProjectBranchPromotionLevel/>
                </Form.Item>
                <Form.Item
                    name="targetPromotionLevel"
                    label="... to promotion level"
                    initialValue={{targetProject, targetBranch, targetPromotionLevel}}
                >
                    <SelectProjectBranchPromotionLevel/>
                </Form.Item>
                <Form.Item
                    name="interval"
                    label="Interval"
                    initialValue={interval}
                >
                    <SelectChartInterval/>
                </Form.Item>
                <Form.Item
                    name="period"
                    label="Period"
                    initialValue={period}
                >
                    <SelectChartPeriod/>
                </Form.Item>
            </Form>
        </>
    )
}