import {Popconfirm, Popover, Spin} from "antd";
import {FaTrashAlt} from "react-icons/fa";
import {useState} from "react";
import graphQLCall from "@client/graphQLCall";
import {gql} from "graphql-request";

export default function PromotionRunDeleteAction({promotionRun, onDeletion}) {

    const [deleting, setDeleting] = useState(false)

    const onDelete = () => {
        setDeleting(true)
        graphQLCall(
            gql`
                mutation DeletePromotionRun(
                    $promotionRunId: Int!,
                ) {
                    deletePromotionRun(input: {
                        promotionRunId: $promotionRunId,
                    }) {
                        errors {
                            message
                        }
                    }
                }
            `,
            {promotionRunId: promotionRun.id}
        ).then(() => {
            if (onDeletion) onDeletion()
        }).finally(() => {
            setDeleting(false)
        })
    }

    return (
        <>
            <Popover content="Deletes this promotion.">
                <Popconfirm
                    title="Deleting a promotion"
                    description="Are you sure to delete this promotion? This cannot be undone."
                    okText="Confirm deletion"
                    okType="danger"
                    onConfirm={onDelete}
                >
                    {
                        deleting ?
                            <Spin size="small"/> :
                            <FaTrashAlt className="ot-command"/>
                    }

                </Popconfirm>
            </Popover>
        </>
    )
}