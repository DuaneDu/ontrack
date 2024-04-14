import {Descriptions, Space, Tag} from "antd";

export default function SlackNotificationChannelOutput({message}) {
    return (
        <>
            <Descriptions
                column={12}
                items={[
                    {
                        key: 'message',
                        label: 'Message',
                        children: <Space>
                            <Tag>markdown</Tag>
                            {message}
                        </Space>,
                        span: 12,
                    },
                ]}/>
        </>
    )
}