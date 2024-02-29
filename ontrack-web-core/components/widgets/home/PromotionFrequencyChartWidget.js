import {useContext, useEffect} from "react";
import {DashboardWidgetCellContext} from "@components/dashboards/DashboardWidgetCellContextProvider";
import {usePromotionLevel} from "@components/widgets/home/promotionChartUtils";
import PromotionLevelFrequencyChart from "@components/promotionLevels/PromotionLevelFrequencyChart";
import PromotionChartTitle from "@components/widgets/home/PromotionChartTitle";

export default function PromotionFrequencyChartWidget({project, branch, promotionLevel, interval, period}) {

    const {setTitle} = useContext(DashboardWidgetCellContext)

    const promotionLevelObject = usePromotionLevel(project, branch, promotionLevel)

    useEffect(() => {
        if (promotionLevelObject) {
            setTitle(
                <>
                    <PromotionChartTitle
                        prefix="Frequency of"
                        project={project}
                        branch={branch}
                        promotionLevel={promotionLevelObject}
                        interval={interval}
                        period={period}
                    />
                </>
            )
        }
    }, [promotionLevelObject]);

    return (
        <>
            {
                promotionLevelObject &&
                <PromotionLevelFrequencyChart
                    promotionLevel={promotionLevelObject}
                    interval={interval}
                    period={period}
                />
            }
        </>
    )
}