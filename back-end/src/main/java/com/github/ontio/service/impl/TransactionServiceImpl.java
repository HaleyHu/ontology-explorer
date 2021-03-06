/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 * The ontology is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ontology is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 */



package com.github.ontio.service.impl;

import com.github.ontio.dao.OntIdMapper;
import com.github.ontio.dao.TransactionDetailMapper;
import com.github.ontio.dao.TransactionMapper;
import com.github.ontio.model.OntId;
import com.github.ontio.paramBean.Result;
import com.github.ontio.service.ITransactionService;
import com.github.ontio.utils.*;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhouq
 * @version 1.0
 * @date 2018/2/27
 */
@Service("TransactionService")
@MapperScan("com.github.ontio.dao")
public class TransactionServiceImpl implements ITransactionService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String VERSION = "1.0";

    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private TransactionDetailMapper transactionDetailMapper;
    @Autowired
    private OntIdMapper ontIdMapper;
    @Autowired
    private ConfigParam configParam;

    private OntologySDKService sdk;

    private synchronized void initSDK() {
        if (sdk == null) {
            sdk = OntologySDKService.getInstance(configParam);
        }
    }

    @Override
    public Result queryTxnList(int amount) {

        List<Map> txnList = transactionMapper.selectNotOntIdTxnByPage(0, amount);
        Map<String, Object> rs = new HashMap();
        rs.put("TxnList", txnList);

        return Helper.result("QueryTransaction", ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), VERSION, rs);
    }

    @Override
    public Result queryTxnList(int pageSize, int pageNumber) {

        int start = pageSize * (pageNumber - 1) < 0 ? 0 : pageSize * (pageNumber - 1);

        List<Map> txnList = transactionMapper.selectNotOntIdTxnByPage(start, pageSize);
        int amount = transactionMapper.selectNotOntIdTxnAmount();

        Map<String, Object> rs = new HashMap();
        rs.put("TxnList", txnList);
        rs.put("Total", amount);

        return Helper.result("QueryTransaction", ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), VERSION, rs);
    }


    @Override
    public Result queryTxnDetailByHash(String txnHash) {

        Map<String, Object> txnInfo = transactionMapper.selectTxnByHash(txnHash);
        if (txnInfo == null) {
            return Helper.result("QueryTransaction", ErrorInfo.NOT_FOUND.code(), ErrorInfo.NOT_FOUND.desc(), VERSION, false);
        }
        String desc = (String) txnInfo.get("Description");
        logger.info("txn desc:{}", desc);
        if (ConstantParam.TRANSFER_OPE.equals(desc)) {

            List<Map> txnDetailList = transactionDetailMapper.selectTxnDetailByHash(txnHash);
            String assetName = (String) txnDetailList.get(0).get("AssetName");
            for (int i = 0; i < txnDetailList.size(); i++) {
                txnDetailList.get(i).remove("AssetName");
            }
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("AssetName", assetName);
            detailMap.put("TransferList", txnDetailList);
            txnInfo.put("Detail", detailMap);
        } else if (desc.startsWith(ConstantParam.ONTID_OPE_PREFIX)) {

            OntId ontIdInfo = ontIdMapper.selectByPrimaryKey(txnHash);
            String ontId = ontIdInfo.getOntid();
            String ontIdDes = ontIdInfo.getDescription();
            logger.info("ontId:{}, description:{}", ontId, ontIdDes);
            ontIdDes = Helper.templateOntIdOperation(ontIdDes);
            Map temp = new HashMap();
            temp.put("OntId", ontId);
            temp.put("Description", ontIdDes);
            txnInfo.put("Detail", temp);
        }

        return Helper.result("QueryTransaction", ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), VERSION, txnInfo);
    }


    @Override
    public Result queryAddressInfo(String address, int pageNumber, int pageSize) {

        Map<String, Object> rs = new HashMap<>();
        List<Map> txnList = new ArrayList<>();

        int start = (pageNumber - 1) * pageSize < 0 ? 0 : (pageNumber - 1) * pageSize;

        Map<String, Object> parmMap = new HashMap<>();
        parmMap.put("Address", address);

        int txnAmount = transactionDetailMapper.selectTxnAmountByAddressInfo(parmMap);

        parmMap.put("Start", start);
        parmMap.put("PageSize", pageSize);
        List<String> txnHashList = transactionDetailMapper.selectTxnHashByAddressInfo(parmMap);
        for (String txnHash :
                txnHashList) {
            Map txnMap = getTransferTxnList(txnHash);
            txnList.add(txnMap);
        }

        List<Object> balanceList = getAddressBalance(address, "");

        rs.put("AssetBalance", balanceList);
        rs.put("TxnList", txnList);
        rs.put("TxnTotal", txnAmount);

        return Helper.result("QueryAddressInfo", ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), VERSION, rs);
    }

    @Override
    public Result queryAddressInfo(String address, int pageNumber, int pageSize, String assetName) {

        Map<String, Object> rs = new HashMap<>();
        List<Map> txnList = new ArrayList<>();

        int start = (pageNumber - 1) * pageSize < 0 ? 0 : (pageNumber - 1) * pageSize;

        Map<String, Object> parmMap = new HashMap<>();
        parmMap.put("Address", address);
        parmMap.put("AssetName", assetName);

        int txnAmount = transactionDetailMapper.selectTxnAmountByAddressInfo(parmMap);

        parmMap.put("Start", start);
        parmMap.put("PageSize", pageSize);
        List<String> txnHashList = transactionDetailMapper.selectTxnHashByAddressInfo(parmMap);
        for (String txnHash :
                txnHashList) {
            Map txnMap = getTransferTxnList(txnHash);
            txnList.add(txnMap);
        }

        List<Object> balanceList = getAddressBalance(address, assetName);

        rs.put("AssetBalance", balanceList);
        rs.put("TxnList", txnList);
        rs.put("TxnTotal", txnAmount);

        return Helper.result("QueryAddressInfo", ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), VERSION, rs);

    }

    /**
     * query transfer information by txnHash
     * and format the information
     *
     * @param txnHash
     * @return
     */
    private Map getTransferTxnList(String txnHash) {

        Map<String, Object> txnMap = transactionMapper.selectTransferTxnByTxnHash(txnHash);
        List<Map> txnDetailList = transactionDetailMapper.selectTxnDetailByHash(txnHash);
        String assetName = (String) txnDetailList.get(0).get("AssetName");
        for (int i = 0; i < txnDetailList.size(); i++) {
            txnDetailList.get(i).remove("AssetName");
        }

        txnMap.put("AssetName", assetName);
        txnMap.put("TransferList", txnDetailList);

        return txnMap;
    }

    /**
     * query balance by address
     * and format the information
     *
     * @param address
     * @return
     */
    private List getAddressBalance(String address, String assetName) {

        List<Object> balanceList = new ArrayList<>();

        initSDK();
        Map<String, Object> balanceMap = sdk.getAddressBalance(address);
        for (Map.Entry<String, Object> entry : balanceMap.entrySet()) {
            Map<String, Object> temp = new HashMap<>();
            if (Helper.isEmptyOrNull(assetName)) {
                temp.put("AssetName", entry.getKey());
                temp.put("Balance", entry.getValue());
                balanceList.add(temp);
            } else if (((String) entry.getKey()).equals(assetName)) {
                temp.put("AssetName", entry.getKey());
                temp.put("Balance", entry.getValue());
                balanceList.add(temp);
            }

        }
        return balanceList;
    }


}
