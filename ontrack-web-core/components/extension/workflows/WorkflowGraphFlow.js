import {addEdge, applyNodeChanges, Background, ControlButton, Controls, ReactFlow} from "reactflow";
import {useCallback, useEffect, useState} from "react";
import WorkflowGraphNode from "@components/extension/workflows/WorkflowGraphNode";
import {autoLayout} from "@components/links/GraphUtils";
import {FaPlusSquare, FaProjectDiagram} from "react-icons/fa";

const nodeTypes = {
    workflowNode: WorkflowGraphNode,
}

export const changeNodes = (node, oldNodes) => {
    // Adjust the node executor & data
    let newNodes = oldNodes.map(oldNode => {
        if (oldNode.id === node.oldId) {
            return {
                ...oldNode,
                data: {
                    ...oldNode.data,
                    executorId: node.executorId,
                    data: node.data,
                }
            }
        } else {
            return oldNode
        }
    })
    // Special case when node ID is renamed (edges must be adjusted)
    if (node.id !== node.oldId) {
        newNodes = newNodes.map(oldNode => {
            if (oldNode.id === node.oldId) {
                // Changing the ID of this node
                return {
                    ...oldNode,
                    id: node.id,
                    data: {
                        ...oldNode.data,
                        id: node.id,
                    }
                }
            } else {
                // Changing the parent ID to the new ID
                return {
                    ...oldNode,
                    data: {
                        ...oldNode.data,
                        parents: oldNode.data.parents.map(oldParent => {
                            if (oldParent.id === node.oldId) {
                                return {id: node.id}
                            } else {
                                return oldParent
                            }
                        })
                    }
                }
            }
        })
    }
    // OK
    return newNodes
}

export const changeEdges = (node, oldEdges) => {
    let newEdges = oldEdges
    if (node.id !== node.oldId) {
        // Adapting the edges
        newEdges = newEdges.map(oldEdge => {
            if (oldEdge.source === node.oldId) {
                return {
                    ...oldEdge,
                    id: `${node.id}-${oldEdge.target}`,
                    source: node.id,
                }
            } else {
                return oldEdge
            }
        })
    }
    // OK
    return newEdges
}

export default function WorkflowGraphFlow({workflowNodes, edition = false}) {

    const onGraphNodeChange = ({node}) => {
        if (node) {
            setNodes(oldNodes => changeNodes(node, oldNodes))
            setEdges(oldEdges => changeEdges(node, oldEdges))
        }
    }

    const createGraphNode = useCallback(
        (node) => {
            return {
                id: node.id,
                position: {x: 0, y: 0},
                data: {...node, edition, onGraphNodeChange},
                type: 'workflowNode',
            }
        }, [edition])

    const [nodes, setNodes] = useState([])
    const [edges, setEdges] = useState([])

    const setGraph = (nodes, edges) => {
        autoLayout({
            nodes,
            edges,
            nodeWidth: 180, // Can be a function
            nodeHeight: 120, // Can be a function
            setNodes,
            setEdges,
        })
    }

    const relayout = () => {
        setGraph(nodes, edges)
    }

    useEffect(() => {
        // Each workflow node ==> node
        const nodes = workflowNodes.map(node => {
            return createGraphNode(node)
        })

        // Edges
        const edges = []
        workflowNodes.forEach(node => {
            node.parents?.forEach(({id}) => {
                edges.push({
                    id: `${id}-${node.id}`,
                    source: id,
                    target: node.id,
                    type: 'smoothstep',
                })
            })
        })

        // Layout for the graph

        setGraph(nodes, edges)

    }, [workflowNodes, createGraphNode]);

    const onNodesChange = useCallback(
        (changes) => setNodes((nds) => applyNodeChanges(changes, nds)),
        [],
    );

    const addNode = () => {
        const nodeNextNumber = nodes.length + 1
        const nodeId = `n${nodeNextNumber}`
        const node = {
            id: nodeId,
            executorId: null,
            data: {},
            parents: [],
        }
        const graphNode = createGraphNode(node)

        setGraph(nodes.concat(graphNode), edges)
    }

    const onConnect = useCallback(
        (connection) => {
            if (edition) {
                const edge = {
                    id: `${connection.source}-${connection.target}`,
                    source: connection.source,
                    target: connection.target,
                    type: 'smoothstep',
                }
                setEdges((oldEdges) => addEdge(edge, oldEdges))
            }
        },
        [edition],
    );

    return (
        <>
            <div style={{height: '600px', width: '100%'}}>
                <ReactFlow
                    nodes={nodes}
                    edges={edges}
                    onNodesChange={onNodesChange}
                    onConnect={onConnect}
                    fitView={true}
                    nodeTypes={nodeTypes}
                >
                    <Background/>
                    <Controls>
                        {
                            edition &&
                            <>
                                <ControlButton title="Adjust the layout" onClick={relayout}>
                                    <FaProjectDiagram/>
                                </ControlButton>
                                <ControlButton title="Add a new start node" onClick={addNode}>
                                    <FaPlusSquare/>
                                </ControlButton>
                            </>
                        }
                    </Controls>
                </ReactFlow>
            </div>
        </>
    )
}