package org.adridadou.ethereum.provider;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import org.adridadou.ethereum.blockchain.BlockchainProxyReal;
import org.adridadou.ethereum.EthereumFacade;
import org.adridadou.ethereum.handler.EthereumEventHandler;
import org.adridadou.ethereum.handler.OnBlockHandler;
import org.adridadou.ethereum.handler.OnTransactionHandler;
import org.adridadou.ethereum.keystore.FileSecureKey;
import org.adridadou.ethereum.keystore.SecureKey;
import org.adridadou.exception.EthereumApiException;
import org.ethereum.config.SystemProperties;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.springframework.context.annotation.Bean;
import org.web3j.crypto.WalletUtils;

/**
 * Created by davidroon on 27.04.16.
 * This code is released under Apache 2 license
 */
public class RopstenEthereumFacadeProvider implements EthereumFacadeProvider {

    private static class TestNetConfig {
        private final String ropstenConfig =
                "peer.discovery = {\n" +
                        "\n" +
                        "    # List of the peers to start\n" +
                        "    # the search of the online peers\n" +
                        "    # values: [ip:port, ip:port, ip:port ...]\n" +
                        "    ip.list = [\n" +
                        "        \"94.242.229.4:40404\",\n" +
                        "        \"94.242.229.203:30303\"\n" +
                        "    ]\n" +
                        "}\n" +
                        "\n" +
                        "# Network id\n" +
                        "peer.networkId = 2\n" +
                        "\n" +
                        "# Enable EIP-8\n" +
                        "peer.p2p.eip8 = true\n" +
                        "\n" +
                        "# the folder resources/genesis\n" +
                        "# contains several versions of\n" +
                        "# genesis configuration according\n" +
                        "# to the network the peer will run on\n" +
                        "genesis = ropsten.json\n" +
                        "\n" +
                        "# Blockchain settings (constants and algorithms) which are\n" +
                        "# not described in the genesis file (like MINIMUM_DIFFICULTY or Mining algorithm)\n" +
                        "blockchain.config.name = \"ropsten\"\n" +
                        "\n" +
                        "database {\n" +
                        "    # place to save physical storage files\n" +
                        "    dir = database-ropsten\n" +
                        "}\n";


        @Bean
        public SystemProperties systemProperties() {
            SystemProperties props = new SystemProperties();
            props.overrideParams(ConfigFactory.parseString(ropstenConfig.replaceAll("'", "\"")));
            return props;
        }
    }

    @Override
    public EthereumFacade create() {
        return create(new OnBlockHandler(), new OnTransactionHandler());
    }

    @Override
    public EthereumFacade create(OnBlockHandler onBlockHandler, OnTransactionHandler onTransactionHandler) {
        Ethereum ethereum = EthereumFactory.createEthereum(TestNetConfig.class);
        EthereumEventHandler ethereumListener = new EthereumEventHandler(ethereum, onBlockHandler, onTransactionHandler);
        ethereum.init();

        return new EthereumFacade(new BlockchainProxyReal(ethereum, ethereumListener));
    }

    @Override
    public SecureKey getKey(final String id) {
        return listAvailableKeys().stream().filter(file -> file.getName().equals(id)).findFirst().orElseThrow(() -> {
            String names = listAvailableKeys().stream().map(FileSecureKey::getName).reduce((aggregate, name) -> aggregate + "," + name).orElse("");
            return new EthereumApiException("could not find the keyfile " + id + " available:" + names);
        });
    }

    private String getKeystoreFolderPath() {
        return WalletUtils.getTestnetKeyDirectory();
    }

    @Override
    public List<FileSecureKey> listAvailableKeys() {
        File[] files = Optional.ofNullable(new File(getKeystoreFolderPath()).listFiles()).orElseThrow(() -> new EthereumApiException("cannot find the folder " + getKeystoreFolderPath()));
        return Lists.newArrayList(files).stream()
                .filter(File::isFile)
                .map(FileSecureKey::new)
                .collect(Collectors.toList());
    }
}