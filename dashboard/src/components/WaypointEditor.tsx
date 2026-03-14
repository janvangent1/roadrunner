'use client';

import { useState } from 'react';
import type { WaypointType } from '@/types';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectTrigger,
  SelectContent,
  SelectItem,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

export interface WaypointRow {
  label: string;
  latitude: string; // string for form input; caller converts to number before submission
  longitude: string;
  type: WaypointType;
}

interface WaypointEditorProps {
  value: WaypointRow[];
  onChange: (rows: WaypointRow[]) => void;
}

const WAYPOINT_TYPES: WaypointType[] = ['FUEL', 'WATER', 'CAUTION', 'INFO'];

export function WaypointEditor({ value, onChange }: WaypointEditorProps) {
  function addRow() {
    onChange([...value, { label: '', latitude: '', longitude: '', type: 'INFO' }]);
  }

  function removeRow(index: number) {
    onChange(value.filter((_, i) => i !== index));
  }

  function updateRow(index: number, field: keyof WaypointRow, val: string) {
    const next = value.map((row, i) => (i === index ? { ...row, [field]: val } : row));
    onChange(next);
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium">Waypoints / POIs</span>
        <Button type="button" variant="outline" size="sm" onClick={addRow}>
          Add waypoint
        </Button>
      </div>
      {value.length === 0 ? (
        <p className="text-sm text-muted-foreground">No waypoints added.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Label</TableHead>
              <TableHead>Latitude</TableHead>
              <TableHead>Longitude</TableHead>
              <TableHead>Type</TableHead>
              <TableHead className="w-16"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {value.map((row, i) => (
              <TableRow key={i}>
                <TableCell>
                  <Input
                    value={row.label}
                    onChange={(e) => updateRow(i, 'label', e.target.value)}
                    placeholder="e.g. Fuel stop"
                  />
                </TableCell>
                <TableCell>
                  <Input
                    value={row.latitude}
                    onChange={(e) => updateRow(i, 'latitude', e.target.value)}
                    placeholder="-33.8688"
                  />
                </TableCell>
                <TableCell>
                  <Input
                    value={row.longitude}
                    onChange={(e) => updateRow(i, 'longitude', e.target.value)}
                    placeholder="151.2093"
                  />
                </TableCell>
                <TableCell>
                  <Select value={row.type} onValueChange={(v) => updateRow(i, 'type', v)}>
                    <SelectTrigger className="w-32">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {WAYPOINT_TYPES.map((t) => (
                        <SelectItem key={t} value={t}>
                          {t}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </TableCell>
                <TableCell>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => removeRow(i)}
                  >
                    Remove
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
