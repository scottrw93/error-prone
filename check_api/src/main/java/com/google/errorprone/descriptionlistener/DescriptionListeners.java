package com.google.errorprone.descriptionlistener;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.hubspot.HubSpotUtils;
import com.google.errorprone.matchers.Description;
import com.sun.tools.javac.util.Context;

public class DescriptionListeners {
  private static final Supplier<CustomDescriptionListenerFactory> CUSTOM_DESCRIPTION_LISTENER_FACTORY_SUPPLIER = Suppliers.memoize(DescriptionListeners::getCustomFactory);

  private DescriptionListeners() {}

  public static DescriptionListener.Factory factory(Context context) {
    return getFactory(context, true);
  }

  public static DescriptionListener.Factory factoryForRefactoring(Context context) {
    return getFactory(context, false);
  }

  private static DescriptionListener.Factory getFactory(Context context, boolean useErrors) {
    return (log, compilation) ->
        CUSTOM_DESCRIPTION_LISTENER_FACTORY_SUPPLIER.get().createFactory(
            DescriptionListenerResources.create(log, compilation, context, useErrors)
        );
  }

  private static CustomDescriptionListenerFactory getCustomFactory() {
    ServiceLoader<CustomDescriptionListenerFactory> serviceLoader =
        ServiceLoader.load(CustomDescriptionListenerFactory.class, DescriptionListeners.class.getClassLoader());
    return new MultiCustomDescriptionListenerFactory(serviceLoader);
  }

  private static class MultiDescriptionListener implements DescriptionListener {
    private final DescriptionListenerResources resources;
    private final List<DescriptionListener> delegates;

    private MultiDescriptionListener(DescriptionListenerResources resources, List<DescriptionListener> delegates) {
      this.resources = resources;
      this.delegates = delegates;
    }

    @Override
    public void onDescribed(Description description) {
      boolean handleErrors = HubSpotUtils.isErrorHandlingEnabled(resources);
      for (DescriptionListener listener : delegates) {
        try {
          listener.onDescribed(description);
        } catch (Throwable t) {
          if (handleErrors) {
            HubSpotUtils.recordListenerDescribeError(listener, t);
          } else {
            throw t;
          }
        }
      }
    }
  }

  private static class MultiCustomDescriptionListenerFactory implements CustomDescriptionListenerFactory {
    private final Iterable<CustomDescriptionListenerFactory> delegates;

    private MultiCustomDescriptionListenerFactory(Iterable<CustomDescriptionListenerFactory> delegates) {
      this.delegates = delegates;
    }

    @Override
    public DescriptionListener createFactory(DescriptionListenerResources resources) {
      final List<DescriptionListener> descriptionListeners;
      if (HubSpotUtils.isErrorHandlingEnabled(resources)) {
        descriptionListeners = HubSpotUtils.loadDescriptionListeners(delegates, resources);
      } else {
        descriptionListeners = new ArrayList<>();
        for (CustomDescriptionListenerFactory delegate : delegates) {
          descriptionListeners.add(delegate.createFactory(resources));
        }
      }
      return new MultiDescriptionListener(resources, descriptionListeners);
    }
  }
}
