import {Drawer, Menu} from "antd";
import {useContext, useEffect, useState} from "react";
import {LogoutOutlined, PoweroffOutlined} from "@ant-design/icons";
import {UserContext} from "@components/providers/UserProvider";
import {logout} from "@client/login";
import {legacyUri} from "@components/common/Links";
import {useRouter} from "next/router";

export function useUserMenu() {
    const [open, setOpen] = useState(false);

    return {
        open,
        setOpen,
    }
}

export default function UserMenu({userMenu}) {

    const router = useRouter()

    const user = useContext(UserContext)
    const [items, setItems] = useState([])

    const createMenuItem = (action) => {
        return {
            key: action.id,
            label: action.name,
            onClick: () => {
                if (action.type === 'LINK' && action.uri) {
                    // TODO Legacy vs. Next UI
                    // noinspection JSIgnoredPromiseFromCall
                    router.push(action.uri)
                } else {
                    console.log(`Unsupported action type ${action.type} for action ${action.id} (${action.name}).`)
                }
            }
        }
    }

    useEffect(() => {
        if (user?.actions) {
            // Menu
            const menu = []
            const topLevelActions = []
            const groupIndex = {}
            // Building the menu from the user actions
            user.actions.forEach(action => {
                const groupName = action.group
                if (groupName) {
                    let group = groupIndex[groupName]
                    if (!group) {
                        group = {
                            key: groupName,
                            label: groupName,
                            // TODO icon
                            children: [],
                        }
                        groupIndex[groupName] = group
                    }
                    group.children.push(createMenuItem(action))
                } else {
                    topLevelActions.push(createMenuItem(action))
                }
            })
            // Top level actions at the start
            menu.push(...topLevelActions)
            // Sorting the groups
            const groups = [];
            for (const [_, group] of Object.entries(groupIndex)) {
                groups.push(group);
            }
            groups.sort((a, b) => {
                const na = a.key;
                const nb = b.key;
                if (na < nb) {
                    return -1;
                } else if (na > nb) {
                    return 1;
                } else {
                    return 0;
                }
            });
            // TODO Sorting items in groups
            // Groups in the menu
            menu.push(...groups)
            // Separator
            menu.push({
                type: 'divider',
            })
            // Adding predefined "Legacy UI"
            menu.push({
                key: 'legacy',
                label: "Legacy UI",
                icon: <PoweroffOutlined/>,
                onClick: () => {
                    location.href = legacyUri()
                }
            })
            // Separator
            menu.push({
                type: 'divider',
            })
            // Adding predefined "Sign out"
            // Not working in local development mode
            menu.push({
                key: 'logout',
                label: "Sign out",
                icon: <LogoutOutlined/>,
                onClick: () => {
                    logout()
                },
            })
            // Registers the menu
            setItems(menu)
        }
    }, [user])

    const onClose = () => {
        userMenu.setOpen(false)
    }

    // const items = [
    //     {
    //         key: 'user-profile',
    //         label: "User profile",
    //         icon: <UserOutlined/>,
    //     },
    //     {
    //         type: 'divider'
    //     },
    //     {
    //         key: 'system',
    //         label: "System",
    //         icon: <SettingOutlined/>,
    //         children: [
    //             {
    //                 key: 'casc',
    //                 label: "Configuration as code",
    //             },
    //             {
    //                 key: 'global-subscriptions',
    //                 label: "Global subscriptions",
    //             },
    //         ],
    //     },
    //     {
    //         type: 'divider'
    //     },
    //     {
    //         key: 'legacy',
    //         label: "Legacy UI",
    //         icon: <Link href={legacyUri()}><PoweroffOutlined /></Link>,
    //     },
    //     {
    //         type: 'divider'
    //     },
    //     {
    //         key: 'logout',
    //         label: "Sign out",
    //         icon: <LogoutOutlined/>,
    //     },
    // ];

    const onClick = (e) => {
        console.log("User menu: ", e.key)
        onClose()
    };

    return (
        <>
            <Drawer placement="right"
                    open={userMenu.open}
                    closable={false}
                    onClose={onClose}
            >
                <Menu mode="inline"
                      selectable={false}
                      items={items}
                      onClick={onClick}
                >
                </Menu>
            </Drawer>
        </>
    )
}
