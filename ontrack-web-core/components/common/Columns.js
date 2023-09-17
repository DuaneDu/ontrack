import {Space} from "antd";

export default function Columns({size = 16, children}) {
    return <Space direction="horizontal"
                  size={size}
                  className="ot-line"
    >
        {children}
    </Space>
}