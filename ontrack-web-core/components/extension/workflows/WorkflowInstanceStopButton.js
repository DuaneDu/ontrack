import {Button, Popconfirm} from "antd";
import {FaStop} from "react-icons/fa";
import {useGraphQLClient} from "@components/providers/ConnectionContextProvider";
import {gql} from "graphql-request";
import {useState} from "react";

export default function WorkflowInstanceStopButton({id, onStopped}) {

    const client = useGraphQLClient()
    const [stopping, setStopping] = useState(false)

    const stop = async () => {
        setStopping(true)
        try {
            await client.request(
                gql`
                    mutation StopWorkflow($id: String!) {
                        stopWorkflow(input: {workflowInstanceId: $id}) {
                            errors {
                                message
                            }
                        }
                    }
                `,
                {id}
            )
            if (onStopped) onStopped()
        } finally {
            setStopping(false)
        }
    }

    return (
        <>
            <Popconfirm title="Are you sure to stop the execution of this workflow?" onConfirm={stop}>
                <Button
                    icon={<FaStop/>}
                    danger
                    title="Stops the execution of this workflow"
                    loading={stopping}
                >
                    Stop
                </Button>
            </Popconfirm>
        </>
    )
}