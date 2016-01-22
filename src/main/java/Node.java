import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

public class Node {
    private NodeInfo self;

    private String masterNode;

    private List<NodeInfo> dictionary;

    private ClientFactoryPDS clientFactoryPDS;

    public Node(String ip) {
        self = new NodeInfo();
        self.setIp(ip);
        dictionary = new ArrayList<NodeInfo>();
        clientFactoryPDS = new ClientFactoryPDS();
    }

    public List<NodeInfo> join(String ipPort) {
        try {
            Host pds = clientFactoryPDS.getClient(ipPort);
            Object[] ipPorts = pds.getHosts(self.getIp());

            NodeInfo nodeInfo = new NodeInfo();
            nodeInfo.setIp(ipPort);
            dictionary.add(nodeInfo);

            for (Object s : ipPorts) {
                nodeInfo = new NodeInfo();
                nodeInfo.setIp((String) s);
                dictionary.add(nodeInfo);
                pds = clientFactoryPDS.getClient(nodeInfo.getIp());
                pds.addNewHost(self.getIp());
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
        System.out.println("joined to " + ipPort);
        return dictionary;
    }

    public void signOff() {
        try {
            for (int i = 0; i < dictionary.size(); i++) {
                Host pds = clientFactoryPDS.getClient(dictionary.get(i).getIp());
                pds.DelHost(self.getIp());
            }
            dictionary.clear();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void start() {
        System.out.println("master node election...");
        NodeInfo MasterNode;
        //MasterNode = electMasterNode();
    }

    /*
        protected NodeInfo electMasterNode() {
            List<UUID> nodeIDs = new ArrayList<UUID>();
            for (NodeInfo nodeInfo : dictionary) {
                if (getSelf().getId().compareTo(nodeInfo.getId()) == -1) {
                    nodeIDs.add(nodeInfo.getId());
                }
            }
            UUID[] nodeIDsArray = new UUID[nodeIDs.size()];
            nodeIDs.toArray(nodeIDsArray);
            Arrays.sort(nodeIDsArray);
            boolean isMaster = true;
            Calendar calendar = Calendar.getInstance();
            java.util.Date now = calendar.getTime();
            if (nodeIDsArray.length != 0) {
                while (masterNode.getIp() == null) {
                    for (int i = nodeIDsArray.length - 1; i >= 0; i--) {
                        for (NodeInfo nodeInfo : dictionary) {
                            if (nodeInfo.getId().equals(nodeIDsArray[i])) {
                                try {
                                    Host pds = clientFactoryPDS.getClient(nodeInfo.getIp());
                                    if (pds.isAlive().equals("Ok")) {
                                        isMaster = false;
                                    }
                                } catch (Exception ex) {
                                    System.out.println(ex);
                                }
                            }
                        }
                    }
                    if (isMaster) {
                        masterNode = getSelf();
                        System.out.println("master node is " + masterNode.getIp());
                        for (NodeInfo node : dictionary) {
                            Host pds = clientFactoryPDS.getClient(node.getIp());
                            pds.masterMessage(self.getIp(), self.getId());
                        }
                        break;
                    }
                }
                try {
                    Thread.sleep(1000);                 //1000 milliseconds is one second.
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } else {
                masterNode = getSelf();
                System.out.println("master node is " + masterNode.getIp());
                for (NodeInfo node : dictionary) {
                    try {
                        Host pds = clientFactoryPDS.getClient(node.getIp());
                        pds.masterMessage(self.getIp(), self.getId());
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
            }
            return masterNode;
        }
    */
    public void CME() {
        System.out.println("Centralised Mutual Exclusion used for connecting to " + getMasterNode());
        for (NodeInfo node : dictionary) {
            try {
                Host pds = clientFactoryPDS.getClient(node.getIp());
                pds.loop();
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }

    public void tram() {
        try {
            Host pds = clientFactoryPDS.getClient(getMasterNode());
            Boolean isAccess;
            String masterStr = pds.getString();
            masterStr += self.getIp();
            pds = clientFactoryPDS.getClient(getMasterNode());
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public NodeInfo getSelf() {
        return self;
    }

    public void setSelf(NodeInfo self) {
        this.self = self;
    }

    public String getMasterNode() {
        return masterNode;
    }

    public void setMasterNode(String masterNode) {
        this.masterNode = masterNode;
    }

    public void setDictionary(List<NodeInfo> dictionary) {
        this.dictionary = dictionary;
    }

    public List<NodeInfo> getDictionary() {
        return dictionary;
    }

    public String[] getIpPorts() {
        String[] ipPorts = new String[dictionary.size()];

        for (int i = 0; i < dictionary.size(); i++) {
            ipPorts[i] = dictionary.get(i).getIp();
        }

        return ipPorts;
    }


    protected String getIpPortById(String id) {
        for (NodeInfo nodeInfo : dictionary) {
            if (nodeInfo.getId().equals(id)) {
                return nodeInfo.getIp();
            }
        }

        return "";
    }

    public void startBullyElection() {
        // Creating an array of IDs that are bigger then this one
        List<String> nodeIDs = new ArrayList<String>();
        for (NodeInfo nodeInfo : dictionary) {
            if (getSelf().getId().compareTo(nodeInfo.getId()) == -1) {
                nodeIDs.add(nodeInfo.getId());
            }
        }
        boolean _isElectionFinished = false;
        if (nodeIDs.size() != 0) {
            String[] nodeIDsArray = new String[nodeIDs.size()];
            nodeIDs.toArray(nodeIDsArray);
            Arrays.sort(nodeIDsArray);
            // end of creating
            boolean flag = false;
            boolean msg;
            for (String id : nodeIDsArray) {
                msg = sendElectionMsg(getIpPortById(id));
                try {
                    Thread.sleep(1000);                 //1000 milliseconds is one second.
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                flag |= msg;
            }
            if (!flag) {
                _isElectionFinished = true;
            }
        } else {
            _isElectionFinished = true;
        }
        if (_isElectionFinished) {
            for (NodeInfo nodeInfo : dictionary) {
                Host pds = clientFactoryPDS.getClient(nodeInfo.getIp());
                pds.setMasterNode(self.getIp());
            }
        }
    }

    public boolean sendElectionMsg(String ipPort) {
        Host pds = clientFactoryPDS.getClient(ipPort); // call proxy.ReceiveElectionMsg() method of target host.
        boolean msg = false;
        msg = pds.receiveElectionMsg(self.getIp()); // the receiver starts "election algorithm" thread and return true;
        return msg;
    }
}