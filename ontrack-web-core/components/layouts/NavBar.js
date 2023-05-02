import {Avatar, Image, Space, Typography} from "antd";
import Link from "next/link";
import {UserOutlined} from "@ant-design/icons";

const {Text} = Typography;

export default function NavBar() {

    const items = [
        {
            key: 'logout',
            label: 'Logout',
        }
    ];

    // noinspection JSValidateTypes
    return (
        <>
            <Space direction="horizontal" size={16}>
                {/* Logo */}
                <Link href="/">
                    <Image width={32} src="/ontrack-128.png" preview={false}/>
                </Link>
                {/* Title + link to home page */}
                <Link href="/">
                    <Text style={{color: "white", fontSize: '175%', verticalAlign: 'middle'}}>Ontrack</Text>
                </Link>
                {/* TODO Search component */}
                {/* TODO Application messages */}
                {/* TODO User name */}
                {/* User menu (drawer) */}
                <Avatar icon={<UserOutlined/>} style={{backgroundColor: 'white', color: 'black'}}/>
            </Space>
        </>
    )
}