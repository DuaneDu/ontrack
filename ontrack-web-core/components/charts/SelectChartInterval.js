import {Select, Typography} from "antd";
import {intervals} from "@components/charts/ChartInterval";

export default function SelectChartInterval({value, onChange}) {

    const options = intervals.map(it => ({
        value: it.id,
        label: <Typography.Text>
            {it.name}
            <Typography.Text disabled>&nbsp;({it.id})</Typography.Text>
        </Typography.Text>
    }))

    return (
        <>
            <Select
                options={options}
                value={value}
                onChange={onChange}
            />
        </>
    )
}