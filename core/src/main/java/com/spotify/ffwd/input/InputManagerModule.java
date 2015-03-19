// $LICENSE
/**
 * Copyright 2013-2014 Spotify AB. All rights reserved.
 *
 * The contents of this file are licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 **/
package com.spotify.ffwd.input;

import io.netty.channel.ChannelInboundHandler;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class InputManagerModule {
    private final List<InputPlugin> DEFAULT_PLUGINS = Lists.newArrayList();

    private final List<InputPlugin> plugins;

    @JsonCreator
    public InputManagerModule(@JsonProperty("plugins") List<InputPlugin> plugins) {
        this.plugins = Optional.of(plugins).or(DEFAULT_PLUGINS);
    }

    public Module module() {
        return new PrivateModule() {
            @Provides
            public List<PluginSource> sources(final Set<PluginSource> sources) {
                return Lists.newArrayList(sources);
            }

            @Override
            protected void configure() {
                bind(ChannelInboundHandler.class).to(InputChannelInboundHandler.class);

                bind(InputManager.class).to(InputManagerImpl.class).in(Scopes.SINGLETON);
                expose(InputManager.class);

                bindPlugins();
            }

            private void bindPlugins() {
                final Multibinder<PluginSource> sources = Multibinder.newSetBinder(binder(), PluginSource.class);

                int i = 0;

                for (final InputPlugin p : plugins) {
                    final Key<PluginSource> k = Key.get(PluginSource.class, Names.named(String.valueOf(i++)));
                    install(p.module(k));
                    sources.addBinding().to(k);
                }
            }
        };
    }

    public static Supplier<InputManagerModule> supplyDefault() {
        return new Supplier<InputManagerModule>() {
            @Override
            public InputManagerModule get() {
                return new InputManagerModule(null);
            }
        };
    }
}