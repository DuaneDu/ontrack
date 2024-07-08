import SearchResultComponent from "@components/framework/search/SearchResultComponent";

export default function Result({data}) {
    return <SearchResultComponent
        title={data.item.commitShort}
        description={data.item.commitMessage}
    />
}