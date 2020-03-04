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

import com.artipie.asto.Content;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Publisher;

/**
 * Content created from {@link RxFile}.
 *
 * @since 0.1
 */
final class RxFileContent implements Content {

    /**
     * File to read content from.
     */
    private final RxFile file;

    /**
     * Ctor.
     *
     * @param file File to read content from.
     */
    RxFileContent(final RxFile file) {
        this.file = file;
    }

    @Override
    public Publisher<ByteBuffer> bytes() {
        return this.file.flow();
    }

    @Override
    public Optional<Long> size() {
        return Optional.empty();
    }
}
