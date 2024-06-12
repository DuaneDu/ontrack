import {Space, Typography} from "antd";

export default function OntrackValidationNotificationChannelConfig({project, branch, build, validation}) {
    return (
        <>
            <Space direction="vertical">
                <Space>
                    Project:
                    <Typography.Text code>{project}</Typography.Text>
                </Space>
                <Space>
                    Branch:
                    <Typography.Text code>{branch}</Typography.Text>
                </Space>
                <Space>
                    Build:
                    <Typography.Text code>{build}</Typography.Text>
                </Space>
                <Space>
                    Validation:
                    <Typography.Text code>{validation}</Typography.Text>
                </Space>
            </Space>
        </>
    )
}