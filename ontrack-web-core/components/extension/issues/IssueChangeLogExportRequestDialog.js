import FormDialog, {useFormDialog} from "@components/form/FormDialog";
import {Button, Card, Form, Input, Space} from "antd";
import SelectIssueExportFormat from "@components/extension/issues/SelectIssueExportFormat";
import {FaPlus, FaTimes, FaTrash} from "react-icons/fa";

export const useIssueChangeLogExportRequestDialog = ({onSuccess}) => {
    return useFormDialog({
        init: (form, context) => {
            form.setFieldValue('format', context.format)
            form.setFieldValue('groups', context.groups)
            form.setFieldValue('exclude', context.exclude)
            form.setFieldValue('altGroup', context.altGroup)
        },
        onSuccess,
    })
}

export default function IssueChangeLogExportRequestDialog({issueChangeLogExportRequestDialog}) {
    return (
        <FormDialog dialog={issueChangeLogExportRequestDialog}>
            <Form.Item
                name="format"
                label="Format"
            >
                <SelectIssueExportFormat/>
            </Form.Item>
            <Form.List name="groups">
                {(fields, {add, remove}) => (
                    <>
                        <div
                            style={{
                                display: 'flex',
                                rowGap: 16,
                                flexDirection: 'column',
                            }}
                        >
                            {fields.map((field) => (
                                <Card
                                    size="small"
                                    title={`Group ${field.name + 1}`}
                                    key={field.key}
                                    headStyle={{
                                        backgroundColor: '#ccc',
                                    }}
                                    bodyStyle={{
                                        backgroundColor: '#eee',
                                    }}
                                    extra={
                                        <FaTrash
                                            onClick={() => {
                                                remove(field.name);
                                            }}
                                        />
                                    }
                                >
                                    <Form.Item label="Group name" name={[field.name, 'name']}>
                                        <Input/>
                                    </Form.Item>

                                    <Form.Item label="List">
                                        <Form.List name={[field.name, 'list']}>
                                            {(subFields, subOpt) => (
                                                <div
                                                    style={{
                                                        display: 'flex',
                                                        flexDirection: 'column',
                                                        rowGap: 16,
                                                    }}
                                                >
                                                    {subFields.map((subField) => (
                                                        <Space key={subField.key}>
                                                            <Form.Item noStyle name={[subField.name, 'mapping']}>
                                                                <Input placeholder="Mapping"/>
                                                            </Form.Item>
                                                            <FaTrash
                                                                onClick={() => {
                                                                    subOpt.remove(subField.name);
                                                                }}
                                                            />
                                                        </Space>
                                                    ))}
                                                    <Button type="dashed" onClick={() => subOpt.add()} block>
                                                        <FaPlus/>
                                                        Add mapping
                                                    </Button>
                                                </div>
                                            )}
                                        </Form.List>
                                    </Form.Item>
                                </Card>
                            ))}
                            <Button type="dashed" onClick={() => add()} block>
                                <FaPlus/>
                                Add group
                            </Button>
                        </div>
                    </>
                )}
            </Form.List>
            <Form.Item
                name="exclude"
                label="Exclude"
                extra="Comma separated list of issue types to exclude from the export."
            >
                <Input/>
            </Form.Item>
            <Form.Item
                name="altGroup"
                label="Alt. group"
                extra={`
                    Title of the group to use when an issue does not belong to any group. It defaults to "Other".
                    If left empty, any issue not belonging to a group would be excluded from the export. This field
                    is not used when no grouping is specified.
                `}
            >
                <Input/>
            </Form.Item>
        </FormDialog>
    )
}