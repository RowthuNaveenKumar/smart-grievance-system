import React from 'react';
import { Card, CardHeader, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import PriorityBadge from './PriorityBadge';
import StatusBadge from './StatusBadge';
import { Calendar, User, Building2, ArrowRight } from 'lucide-react';
import { format } from 'date-fns';
import { motion } from 'framer-motion';

export default function ComplaintCard({ complaint, onClick, showAssignment = false }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={{ y: -2 }}
      transition={{ duration: 0.2 }}
    >
      <Card 
        className="border-slate-200 hover:shadow-xl transition-all cursor-pointer bg-white"
        onClick={onClick}
      >
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1">
              <h3 className="font-semibold text-slate-900 text-lg mb-2 line-clamp-1">
                {complaint.title}
              </h3>
              <p className="text-slate-600 text-sm line-clamp-2 leading-relaxed">
                {complaint.description}
              </p>
            </div>
            <div className="flex gap-2">
              <PriorityBadge priority={complaint.priority} />
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-center justify-between">
            <StatusBadge status={complaint.status} />
            <div className="flex items-center gap-2 text-xs text-slate-500">
              <Calendar className="h-3 w-3" />
              {format(new Date(complaint.created_date), 'MMM d, yyyy')}
            </div>
          </div>

          <div className="flex flex-wrap gap-2 text-xs text-slate-600">
            {complaint.department_name && (
              <div className="flex items-center gap-1 bg-slate-50 px-2 py-1 rounded-md">
                <Building2 className="h-3 w-3" />
                <span>{complaint.department_name}</span>
              </div>
            )}
            {showAssignment && complaint.assigned_staff_name && (
              <div className="flex items-center gap-1 bg-blue-50 px-2 py-1 rounded-md">
                <User className="h-3 w-3 text-blue-600" />
                <span className="text-blue-700">{complaint.assigned_staff_name}</span>
              </div>
            )}
          </div>

          <div className="pt-2">
            <Button variant="ghost" size="sm" className="w-full group">
              View Details
              <ArrowRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />
            </Button>
          </div>
        </CardContent>
      </Card>
    </motion.div>
  );
}