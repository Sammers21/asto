/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.asto.fs;

import com.artipie.asto.Remaining;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.file.CopyOptions;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.file.FileSystem;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The reactive file allows you to perform read and write operations via {@link RxFile#flow()}
 * and {@link RxFile#save(Flowable)} methods respectively.
 * <p>
 * The implementation is based on Vert.x {@link io.vertx.reactivex.core.file.AsyncFile}.
 *
 * @since 0.12
 */
public class VertxRxFile {

    /**
     * The file location of file system.
     */
    private final Path file;

    /**
     * The file system.
     */
    private final FileSystem fls;

    /**
     * Ctor.
     * @param file The wrapped file.
     * @param fls The file system.
     */
    public VertxRxFile(final Path file, final FileSystem fls) {
        this.file = file;
        this.fls = fls;
    }

    /**
     * Read file content as a flow of bytes.
     * @return A flow of bytes
     */
    public Flowable<ByteBuffer> flow() {
        return this.fls.rxOpen(
            this.file.toString(),
            new OpenOptions()
                .setRead(true)
                .setWrite(false)
                .setCreate(false)
        )
            .flatMapPublisher(
                asyncFile -> {
                    final Promise<Void> promise = Promise.promise();
                    final Completable completable = Completable.create(
                        emitter ->
                            promise.future().onComplete(
                                event -> {
                                    if (event.succeeded()) {
                                        emitter.onComplete();
                                    } else {
                                        emitter.onError(event.cause());
                                    }
                                }
                            )
                    );
                    return asyncFile.toFlowable().map(
                        buffer -> ByteBuffer.wrap(buffer.getBytes())
                    ).doOnTerminate(() -> asyncFile.rxClose().subscribe(promise::complete))
                        .mergeWith(completable);
                }
            );
    }

    /**
     * Save a flow of bytes to a file.
     *
     * @param flow The flow of bytes
     * @return Completion or error signal
     */
    public Completable save(final Flowable<ByteBuffer> flow) {
        return this.fls.rxOpen(
            this.file.toString(),
            new OpenOptions()
                .setRead(false)
                .setCreate(true)
                .setWrite(true)
                .setTruncateExisting(true)
        )
            .flatMapCompletable(
                asyncFile -> Completable.create(
                    emitter -> {
                        flow.map(buf -> Buffer.buffer(new Remaining(buf).bytes()))
                            .onErrorResumeNext(
                                thr -> {
                                    return asyncFile.rxClose().andThen(Flowable.error(thr));
                                }
                            )
                            .subscribe(asyncFile.toSubscriber()
                                .onWriteStreamEnd(emitter::onComplete)
                                .onWriteStreamError(emitter::onError)
                                .onWriteStreamEndError(emitter::onError)
                                .onError(emitter::onError)
                            );
                    }
                )
            );
    }

    /**
     * Move file to new location.
     *
     * @param target Target path the file is moved to.
     * @return Completion or error signal
     */
    public Completable move(final Path target) {
        return this.fls.rxMove(
            this.file.toString(),
            target.toString(),
            new CopyOptions().setReplaceExisting(true)
        );
    }

    /**
     * Delete file.
     *
     * @return Completion or error signal
     */
    public Completable delete() {
        return this.fls.rxDelete(this.file.toString());
    }

    /**
     * Get file size.
     *
     * @return File size in bytes.
     */
    public Single<Long> size() {
        return Single.fromCallable(() -> Files.size(this.file));
    }
}