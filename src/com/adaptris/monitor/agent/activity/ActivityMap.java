package com.adaptris.monitor.agent.activity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.adaptris.profiler.ProcessStep;
import com.adaptris.profiler.aspects.InterlokComponent;
import com.adaptris.profiler.aspects.InterlokComponent.ComponentType;

public class ActivityMap implements Serializable {

  private static final long serialVersionUID = 2523877428476982945L;

  private Map<String, BaseActivity> adapters;

  public ActivityMap() {
    adapters = new HashMap<>();
  }

  public void addActivity(ProcessStep activity) {
    InterlokComponent interlokComponent = activity.getInterlokComponent();

    if (!interlokComponent.getComponentType().equals(ComponentType.ServiceList)) {
      InterlokComponent adapterComponent = null;

      try {
        if (interlokComponent.getComponentType().equals(ComponentType.Adapter)) {
          adapterComponent = interlokComponent;
        } else if (interlokComponent.getComponentType().equals(ComponentType.Channel)) {
          adapterComponent = interlokComponent.getParent();
        } else if (interlokComponent.getComponentType().equals(ComponentType.Workflow)) {
          adapterComponent = interlokComponent.getParent().getParent();
        } else if (interlokComponent.getComponentType().equals(ComponentType.Service)) {
          adapterComponent = interlokComponent.getParent().getParent().getParent();
        } else if (interlokComponent.getComponentType().equals(ComponentType.Producer)) {
          adapterComponent = interlokComponent.getParent().getParent().getParent();
        } else if (interlokComponent.getComponentType().equals(ComponentType.Consumer)) {
          adapterComponent = interlokComponent.getParent().getParent().getParent();
        }
      } catch (Throwable ex) {
        ex.printStackTrace();
      }
      if (adapterComponent != null) {
        AdapterActivity storedAdapterActivity = getAdapters().get(adapterComponent.getUniqueId());
        if (storedAdapterActivity == null) {
          AdapterActivity adapterActivity = new AdapterActivity();
          adapterActivity.setUniqueId(adapterComponent.getUniqueId());
          applyComponents(adapterActivity, interlokComponent, activity);
          getAdapters().put(adapterActivity.getUniqueId(), adapterActivity);
        } else {
          applyComponents(storedAdapterActivity, interlokComponent, activity);
        }
      }
    }
  }

  private void applyComponents(AdapterActivity adapterActivity, InterlokComponent interlokComponent, ProcessStep step) {
    if (interlokComponent.getComponentType().equals(ComponentType.Service)) {
      InterlokComponent channelComponent = interlokComponent.getParent().getParent();
      ChannelActivity channelActivity = adapterActivity.getChannels().get(channelComponent.getUniqueId());
      if (channelActivity == null) {
        channelActivity = buildChannelActivity(channelComponent, adapterActivity);
        adapterActivity.getChannels().put(channelActivity.getUniqueId(), channelActivity);
      }

      InterlokComponent workflowComponent = interlokComponent.getParent();
      WorkflowActivity workflowActivity = channelActivity.getWorkflows().get(workflowComponent.getUniqueId());
      if (workflowActivity == null) {
        workflowActivity = buildWorkflowActivity(workflowComponent, channelActivity);
        channelActivity.getWorkflows().put(workflowActivity.getUniqueId(), workflowActivity);
      }
      if (!workflowActivity.getMessageIds().contains(step.getMessageId())) {
        workflowActivity.addMessageId(step.getMessageId());
      }

      InterlokComponent serviceComponent = interlokComponent;
      ServiceActivity serviceActivity = workflowActivity.getServices().get(serviceComponent.getUniqueId());
      if (serviceActivity == null) {
        serviceActivity = buildServiceActivity(serviceComponent, workflowActivity);
        workflowActivity.getServices().put(serviceActivity.getUniqueId(), serviceActivity);
      }

      serviceActivity.addMessageId(step.getMessageId(), step.getTimeTakenMs());
      serviceActivity.setMessageCount(serviceActivity.getMessageCount() + 1);
      serviceActivity.setClassName(step.getStepName());

      long avgMsTaken = calculateAvgMsTaken(serviceActivity.getMsTaken());
      serviceActivity.setAvgMsTaken(avgMsTaken);

    } else if (interlokComponent.getComponentType().equals(ComponentType.Producer)) {
      InterlokComponent channelComponent = interlokComponent.getParent().getParent();
      ChannelActivity channelActivity = adapterActivity.getChannels().get(channelComponent.getUniqueId());
      if (channelActivity == null) {
        channelActivity = buildChannelActivity(channelComponent, adapterActivity);
        adapterActivity.getChannels().put(channelActivity.getUniqueId(), channelActivity);
      }

      InterlokComponent workflowComponent = interlokComponent.getParent();
      WorkflowActivity workflowActivity = channelActivity.getWorkflows().get(workflowComponent.getUniqueId());
      if (workflowActivity == null) {
        workflowActivity = buildWorkflowActivity(workflowComponent, channelActivity);
        channelActivity.getWorkflows().put(workflowActivity.getUniqueId(), workflowActivity);
      }
      if (!workflowActivity.getMessageIds().contains(step.getMessageId())) {
        workflowActivity.addMessageId(step.getMessageId());
      }

      ProducerActivity producerActivity = workflowActivity.getProducerActivity();
      if (producerActivity == null) {
        producerActivity = buildProducerActivity(interlokComponent, workflowActivity);
        workflowActivity.setProducerActivity(producerActivity);
      }

      producerActivity.addMessageId(step.getMessageId(), step.getTimeTakenMs());
      producerActivity.setMessageCount(producerActivity.getMessageCount() + 1);

      long avgMsTaken = calculateAvgMsTaken(producerActivity.getMsTaken());
      producerActivity.setAvgMsTaken(avgMsTaken);

    } else if (interlokComponent.getComponentType().equals(ComponentType.Consumer)) {
      InterlokComponent channelComponent = interlokComponent.getParent().getParent();
      ChannelActivity channelActivity = adapterActivity.getChannels().get(channelComponent.getUniqueId());
      if (channelActivity == null) {
        channelActivity = buildChannelActivity(channelComponent, adapterActivity);
        adapterActivity.getChannels().put(channelActivity.getUniqueId(), channelActivity);
      }

      InterlokComponent workflowComponent = interlokComponent.getParent();
      WorkflowActivity workflowActivity = channelActivity.getWorkflows().get(workflowComponent.getUniqueId());
      if (workflowActivity == null) {
        workflowActivity = buildWorkflowActivity(workflowComponent, channelActivity);
        channelActivity.getWorkflows().put(workflowActivity.getUniqueId(), workflowActivity);
      }
      if (!workflowActivity.getMessageIds().contains(step.getMessageId())) {
        workflowActivity.addMessageId(step.getMessageId());
      }

      ConsumerActivity consumerActivity = workflowActivity.getConsumerActivity();
      if (consumerActivity == null) {
        consumerActivity = buildConsumerActivity(interlokComponent, workflowActivity);
        workflowActivity.setConsumerActivity(consumerActivity);
      }

      consumerActivity.addMessageId(step.getMessageId(), step.getTimeTakenMs());
      consumerActivity.setMessageCount(consumerActivity.getMessageCount() + 1);

      long avgMsTaken = calculateAvgMsTaken(consumerActivity.getMsTaken());
      consumerActivity.setAvgMsTaken(avgMsTaken);

    }
  }

  private ChannelActivity buildChannelActivity(InterlokComponent channelComponent, AdapterActivity adapterActivity) {
    ChannelActivity channelActivity = new ChannelActivity();
    channelActivity.setUniqueId(channelComponent.getUniqueId());
    channelActivity.setParent(adapterActivity);
    return channelActivity;
  }

  private WorkflowActivity buildWorkflowActivity(InterlokComponent workflowComponent, ChannelActivity channelActivity) {
    WorkflowActivity workflowActivity = new WorkflowActivity();
    workflowActivity.setUniqueId(workflowComponent.getUniqueId());
    workflowActivity.setParent(channelActivity);
    return workflowActivity;
  }

  private ConsumerActivity buildConsumerActivity(InterlokComponent consumerComponent, WorkflowActivity workflowActivity) {
    ConsumerActivity consumerActivity = new ConsumerActivity();
    consumerActivity.setUniqueId(consumerComponent.getUniqueId());
    consumerActivity.setParent(workflowActivity);
    consumerActivity.setDestination(consumerComponent.getDestination());
    consumerActivity.setClassName(consumerComponent.getClassName());
    consumerActivity.setVendorImpClass(consumerComponent.getVendorImp());
    return consumerActivity;
  }

  private ProducerActivity buildProducerActivity(InterlokComponent producerComponent, WorkflowActivity workflowActivity) {
    ProducerActivity producerActivity = new ProducerActivity();
    producerActivity.setUniqueId(producerComponent.getUniqueId());
    producerActivity.setParent(workflowActivity);
    producerActivity.setDestination(producerComponent.getDestination());
    producerActivity.setClassName(producerComponent.getClassName());
    producerActivity.setVendorImpClass(producerComponent.getVendorImp());
    return producerActivity;
  }

  private ServiceActivity buildServiceActivity(InterlokComponent serviceComponent, WorkflowActivity workflowActivity) {
    ServiceActivity serviceActivity = new ServiceActivity();
    serviceActivity.setUniqueId(serviceComponent.getUniqueId());
    serviceActivity.setParent(workflowActivity);
    return serviceActivity;
  }

  private long calculateAvgMsTaken(List<Long> msTakens) {
    long totalTaken = 0;
    for(long msTaken : msTakens) {
      totalTaken += msTaken;
    }
    return totalTaken / msTakens.size();
  }

  public Map<String, BaseActivity> getAdapters() {
    return adapters;
  }

  public void setAdapters(Map<String, BaseActivity> adapters) {
    this.adapters = adapters;
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    for(BaseActivity adapter : getAdapters().values()) {
      buffer.append(adapter.toString());
    }
    return buffer.toString();
  }

}
