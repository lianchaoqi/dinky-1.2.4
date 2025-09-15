/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import { Tabs, TabsProps } from 'antd';
import { ProForm, ProFormDigit, ProFormGroup, ProFormSwitch } from '@ant-design/pro-components';
import { l } from '@/utils/intl';
import React from 'react';
import { InfoCircleOutlined } from '@ant-design/icons';
import { DIALECT, SWITCH_OPTIONS } from '@/services/constants';
import { TaskState, TempData } from '@/pages/DataStudio/type';
import { BasicConfig } from '@/pages/DataStudio/CenterTabContent/SqlTask/TaskConfig/BasicConfig';
import { isSql, assert } from '@/pages/DataStudio/utils';
import { JOB_LIFE_CYCLE } from '@/pages/DevOps/constants';

export default (props: {
  tempData: TempData;
  data: TaskState;
  onValuesChange?: (changedValues: any, values: TaskState) => void;
  setCurrentState?: (values: TaskState) => void;
  isLockTask: boolean;
}) => {
  const { data, tempData } = props;
  const items: TabsProps['items'] = [];
  if (assert(data.dialect, [DIALECT.FLINK_SQL, DIALECT.FLINKJAR], true, 'includes')) {
    items.push({
      key: 'basicConfig',
      label: l('menu.datastudio.task.baseConfig'),
      children: (
        <BasicConfig
          tempData={tempData}
          data={props.data}
          onValuesChange={props.onValuesChange}
          setCurrentState={props.setCurrentState}
          isLockTask={props.isLockTask}
        />
      )
    });
  }
  if (
    isSql(data.dialect) ||
    assert(data.dialect, [DIALECT.FLINK_SQL, DIALECT.FLINKJAR], true, 'includes')
  ) {
    const renderOtherConfig = () => {
      if (!isSql(data.dialect)) {
        return (
          <>
            <ProFormSwitch
              label={l('pages.datastudio.label.execConfig.changelog')}
              name='useChangeLog'
              tooltip={{
                title: l('pages.datastudio.label.execConfig.changelog.tip'),
                icon: <InfoCircleOutlined />
              }}
              {...SWITCH_OPTIONS()}
            />
            <ProFormSwitch
              label={l('pages.datastudio.label.execConfig.autostop')}
              name='useAutoCancel'
              tooltip={{
                title: l('pages.datastudio.label.execConfig.autostop.tip'),
                icon: <InfoCircleOutlined />
              }}
              {...SWITCH_OPTIONS()}
            />
            <ProFormSwitch
              label={l('pages.datastudio.label.execConfig.mocksink')}
              name='mockSinkFunction'
              tooltip={{
                title: l('pages.datastudio.label.execConfig.mocksink.tip'),
                icon: <InfoCircleOutlined />
              }}
              {...SWITCH_OPTIONS()}
            />
          </>
        );
      }
    };
    items.push({
      key: 'previewConfig',
      label: l('menu.datastudio.task.previewConfig'),
      children: (
        <ProForm
          className={'datastudio-theme'}
          initialValues={{
            ...props.data
          }}
          disabled={props.data?.step === JOB_LIFE_CYCLE.PUBLISH || props.isLockTask}
          style={{ padding: '10px' }}
          submitter={false}
          layout='vertical'
          onValuesChange={props.onValuesChange}
        >
          <ProFormGroup style={{ display: 'flex', justifyContent: 'center' }}>
            {renderOtherConfig()}
            <ProFormDigit
              width={'xs'}
              label={l('pages.datastudio.label.execConfig.maxrow')}
              name='maxRowNum'
              tooltip={l('pages.datastudio.label.execConfig.maxrow.tip')}
              min={1}
              max={9999}
            />
          </ProFormGroup>
        </ProForm>
      )
    });
  }

  return <Tabs items={items} centered />;
};
