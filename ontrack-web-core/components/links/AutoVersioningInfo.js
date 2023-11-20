import {Descriptions, Popover, Space, Spin, Tooltip, Typography} from "antd";
import {FaCalendar, FaCheck, FaCodeBranch, FaMedal, FaTag, FaThumbsUp, FaTimes} from "react-icons/fa";
import BuildRef from "@components/links/BuildRef";
import CheckIcon from "@components/common/CheckIcon";
import Rows from "@components/common/Rows";
import Columns from "@components/common/Columns";
import Timestamp from "@components/common/Timestamp";
import Link from "next/link";

export default function AutoVersioningInfo({autoVersioning, branchLink}) {
    return (
        <>
            <Space direction="vertical" className="ot-line">
                {/* Last eligible build */}
                {
                    autoVersioning.lastEligibleBuild &&
                    <Space>
                        <FaMedal title="Last eligible build"/>
                        <BuildRef build={autoVersioning.lastEligibleBuild}/>
                        <CheckIcon value={branchLink.targetBuild.id === autoVersioning.lastEligibleBuild.id}/>
                    </Space>
                }
                {/* AV status */}
                {
                    autoVersioning.status &&
                    <Space>
                        {/* Running indicator */}
                        {
                            autoVersioning.status.running ?
                                <Spin size="small"/> :
                                <FaThumbsUp/>
                        }
                        {/* Most recent state */}
                        <Tooltip
                            color="#eee"
                            title={
                                <Rows>
                                    <Columns>
                                        <FaTag color="black"/>
                                        <Typography.Text>Version</Typography.Text>
                                        <Typography.Text
                                            strong>{autoVersioning.status.order.targetVersion}</Typography.Text>
                                    </Columns>
                                    <Columns>
                                        <FaCalendar color="black"/>
                                        <Typography.Text>Created at</Typography.Text>
                                        <Typography.Text strong>
                                            <Timestamp value={autoVersioning.status.mostRecentState.creation.time}/>
                                        </Typography.Text>
                                    </Columns>
                                    {
                                        autoVersioning.status.mostRecentState.data?.prName &&
                                        autoVersioning.status.mostRecentState.data?.prLink &&
                                        <Columns>
                                            <FaCodeBranch color="black"/>
                                            <Typography.Text>PR</Typography.Text>
                                            <Typography.Text>
                                                <Link
                                                    href={autoVersioning.status.mostRecentState.data.prLink}>{autoVersioning.status.mostRecentState.data.prName}</Link>
                                            </Typography.Text>
                                        </Columns>
                                    }
                                </Rows>
                            }
                        >
                            <Space>
                                {autoVersioning.status.mostRecentState.state}
                            </Space>
                        </Tooltip>
                    </Space>
                }
                {/*

                    status {
                        order {
                            targetVersion
                        }
                        running
                        mostRecentState {
                          state
                          running
                          processing
                          creation {
                              time
                          }
                        }
                    }
                */}
            </Space>
        </>
    )
}