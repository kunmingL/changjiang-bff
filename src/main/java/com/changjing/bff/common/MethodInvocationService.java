package com.changjing.bff.common;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MethodInvocationService {

//    @Autowired
//    private CrpcTransferService crpcTransferService;
//
//    public MethodInvocationService() {
//    }
//
//    public ResponseObject<Object> executeMethodInvocation(RequestObject requestObject) throws NpcsGTWException {
//        ResponseObject<Object> responseObject = this.crpcTransferService.methodInvocation(requestObject);
//        if (requestObject.isDataMask() && responseObject.getResData() != null) {
//            if (responseObject.getResData() instanceof Collection) {
//                List<Object> retList = ((Collection) responseObject.getResData()).stream().map(NpcsDataMaskUtil::doDataMask).collect(Collectors.toList());
//                responseObject.setResData(retList);
//            } else if (responseObject.getResData() instanceof Object) {
//                Object encryptedData = NpcsDataMaskUtil.doDataMask((Object) responseObject.getResData());
//                responseObject.setResData(encryptedData);
//            }
//        }
//        return responseObject;
//    }
}
