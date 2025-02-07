package com.changjing.bff.grpc;

import com.changjiang.grpc.lib.GrpcRequest;
import com.changjiang.grpc.lib.GrpcResponse;
import com.changjiang.grpc.lib.GrpcServiceGrpc;
import io.grpc.stub.StreamObserver;

public class ServerGrpcService extends GrpcServiceGrpc.GrpcServiceImplBase {
    @Override
    public void invoke(GrpcRequest request, StreamObserver<GrpcResponse> responseObserver) {
// 处理请求逻辑
        GrpcResponse response = GrpcResponse.newBuilder()
                .setCode(200)
                .setMessage("Success")
                .setPayload(yourProcessedData)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
