import TransitionBox from "@components/common/TransitionBox";
import {buildLink} from "@components/common/Links";
import {FaBan} from "react-icons/fa";
import PromotionLevelLink from "@components/promotionLevels/PromotionLevelLink";

export default function PromotionRunBox({promotionLevel}) {
    const run = promotionLevel.promotionRuns ? promotionLevel.promotionRuns[0] : undefined
    const build = run?.build
    return (
        <>
            <TransitionBox
                before={
                    <PromotionLevelLink promotionLevel={promotionLevel}/>
                }
                after={
                    build ? buildLink(build) : <FaBan/>
                }
            />
        </>
    )
}