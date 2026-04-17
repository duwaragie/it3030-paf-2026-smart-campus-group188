import { useState, useEffect } from 'react';
import AppLayout from '@/components/layout/AppLayout';
import { resourceService, type ResourceDTO, type ResourceSearchParams } from '@/services/resourceService';
import { assetService, type AssetDTO } from '@/services/assetService';
import { amenityService, type AmenityDTO } from '@/services/amenityService';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

const TYPES = ['LECTURE_HALL', 'LAB', 'MEETING_ROOM', 'EQUIPMENT'] as const;
const STATUSES = ['ACTIVE', 'OUT_OF_SERVICE', 'UNDER_MAINTENANCE'] as const;

const typeLabels: Record<string, string> = {
  LECTURE_HALL: 'Lecture Hall',
  LAB: 'Lab',
  MEETING_ROOM: 'Meeting Room',
  EQUIPMENT: 'Equipment',
};

const statusStyle: Record<string, string> = {
  ACTIVE: 'bg-emerald-100 text-emerald-700',
  OUT_OF_SERVICE: 'bg-red-100 text-red-700',
  UNDER_MAINTENANCE: 'bg-amber-100 text-amber-700',
};

export default function FacilitiesCataloguePage() {
  const [resources, setResources] = useState<ResourceDTO[]>([]);
  const [availableAssets, setAvailableAssets] = useState<AssetDTO[]>([]);
  const [availableAmenities, setAvailableAmenities] = useState<AmenityDTO[]>([]);
  
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [searchParams, setSearchParams] = useState<ResourceSearchParams>({ status: 'ACTIVE' });
  const [isSearching, setIsSearching] = useState(false);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { 
    void loadData(); 
  }, []);

  const loadData = async () => {
    try {
      setIsLoading(true);
      setError(null);
      
      const [resResources, resAssets, resAmenities] = await Promise.all([
        resourceService.search({ status: 'ACTIVE' }),
        assetService.getAll().catch(() => ({ data: [] })),
        amenityService.getAll().catch(() => ({ data: [] }))
      ]);
      
      setResources(resResources.data);
      setAvailableAssets(resAssets.data);
      setAvailableAmenities(resAmenities.data);
    } catch {
      setError('Failed to load initial data.');
    } finally {
      setIsLoading(false);
    }
  };

  const executeSearch = async (params: ResourceSearchParams) => {
    try {
      setIsLoading(true);
      setError(null);
      const cleanParams = Object.fromEntries(
        Object.entries(params).filter(([, v]) => {
          if (Array.isArray(v)) return v.length > 0;
          return v !== '' && v != null;
        })
      );
      
      const res = Object.keys(cleanParams).length > 0
        ? await resourceService.search(cleanParams)
        : await resourceService.getAll();
        
      setResources(res.data);
    } catch {
      setError('Failed to search resources.');
    } finally {
      setIsLoading(false);
      setIsSearching(false);
    }
  };

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsSearching(true);
    void executeSearch(searchParams);
  };

  const resetFilters = () => {
    const defaultParams = { status: 'ACTIVE' };
    setSearchParams(defaultParams);
    void executeSearch(defaultParams);
  };

  const toggleSearchAsset = (id: number) => {
    setSearchParams(prev => {
      const currentIds = prev.assetIds || [];
      return {
        ...prev,
        assetIds: currentIds.includes(id) 
          ? currentIds.filter(a => a !== id) 
          : [...currentIds, id]
      };
    });
  };

  const toggleSearchAmenity = (id: number) => {
    setSearchParams(prev => {
      const currentIds = prev.amenityIds || [];
      return {
        ...prev,
        amenityIds: currentIds.includes(id) 
          ? currentIds.filter(a => a !== id) 
          : [...currentIds, id]
      };
    });
  };

  return (
    <AppLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-campus-900">Campus Facilities & Assets</h1>
          <p className="text-sm text-gray-500 mt-1">Browse available lecture halls, labs, meeting rooms, and equipment. Filter by required assets and amenities.</p>
        </div>

        {error && (
          <div className="p-3.5 rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm font-medium flex items-center gap-2">
            <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><circle cx="12" cy="12" r="10" /><path d="M12 8v4m0 4h.01" /></svg>
            {error}
          </div>
        )}

        {/* Filter Bar */}
        <div className="bg-white rounded-2xl border border-gray-100 p-4 shadow-sm">
          <form onSubmit={handleSearch} className="space-y-4">
            <div className="flex flex-wrap items-end gap-4">
              <div className="flex-1 min-w-[150px]">
                <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Type</label>
                <select 
                  value={searchParams.type || ''} 
                  onChange={(e) => setSearchParams({...searchParams, type: e.target.value})}
                  className="w-full h-10 px-3 rounded-lg border border-gray-200 text-sm bg-gray-50/50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-campus-200 transition-colors"
                >
                  <option value="">All Types</option>
                  {TYPES.map((t) => <option key={t} value={t}>{typeLabels[t]}</option>)}
                </select>
              </div>
              
              <div className="flex-1 min-w-[150px]">
                <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Status</label>
                <select 
                  value={searchParams.status || ''} 
                  onChange={(e) => setSearchParams({...searchParams, status: e.target.value})}
                  className="w-full h-10 px-3 rounded-lg border border-gray-200 text-sm bg-gray-50/50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-campus-200 transition-colors"
                >
                  <option value="">All Statuses</option>
                  {STATUSES.map((s) => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
                </select>
              </div>

              <div className="flex-1 min-w-[150px]">
                <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Location</label>
                <input 
                  type="text"
                  placeholder="e.g. Block A"
                  value={searchParams.location || ''} 
                  onChange={(e) => setSearchParams({...searchParams, location: e.target.value})}
                  className="w-full h-10 px-3 rounded-lg border border-gray-200 text-sm bg-gray-50/50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-campus-200 transition-colors placeholder:text-gray-400"
                />
              </div>

              <div className="w-[120px]">
                <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Min. Capacity</label>
                <input 
                  type="number"
                  min="1"
                  placeholder="e.g. 50"
                  value={searchParams.minCapacity || ''} 
                  onChange={(e) => setSearchParams({...searchParams, minCapacity: e.target.value ? Number(e.target.value) : undefined})}
                  className="w-full h-10 px-3 rounded-lg border border-gray-200 text-sm bg-gray-50/50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-campus-200 transition-colors placeholder:text-gray-400"
                />
              </div>

              <div className="flex gap-2">
                <button 
                  type="submit" 
                  disabled={isSearching}
                  className="h-10 px-5 bg-campus-100 hover:bg-campus-200 text-campus-800 text-sm font-bold rounded-lg transition-colors flex items-center gap-2 disabled:opacity-60"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  {isSearching ? 'Searching...' : 'Search'}
                </button>
                
                <button 
                  type="button" 
                  onClick={resetFilters}
                  className="h-10 px-4 border border-gray-200 hover:bg-gray-50 text-gray-600 text-sm font-semibold rounded-lg transition-colors"
                >
                  Reset
                </button>
              </div>
            </div>

            {/* Advanced Filters: Assets and Amenities */}
            <div className="pt-3 border-t border-gray-50 flex flex-col md:flex-row gap-6">
              {availableAssets.length > 0 && (
                <div className="flex-1">
                  <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Required Assets</label>
                  <div className="flex flex-wrap gap-2">
                    {availableAssets.map(asset => (
                      <label key={asset.id} className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-md border text-xs font-medium cursor-pointer transition-colors ${searchParams.assetIds?.includes(asset.id) ? 'bg-blue-50 border-blue-200 text-blue-700' : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-white'}`}>
                        <input 
                          type="checkbox" 
                          className="sr-only"
                          checked={searchParams.assetIds?.includes(asset.id) || false}
                          onChange={() => toggleSearchAsset(asset.id)}
                        />
                        {asset.name}
                      </label>
                    ))}
                  </div>
                </div>
              )}

              {availableAmenities.length > 0 && (
                <div className="flex-1">
                  <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Required Amenities</label>
                  <div className="flex flex-wrap gap-2">
                    {availableAmenities.map(amenity => (
                      <label key={amenity.id} className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-md border text-xs font-medium cursor-pointer transition-colors ${searchParams.amenityIds?.includes(amenity.id) ? 'bg-purple-50 border-purple-200 text-purple-700' : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-white'}`}>
                        <input 
                          type="checkbox" 
                          className="sr-only"
                          checked={searchParams.amenityIds?.includes(amenity.id) || false}
                          onChange={() => toggleSearchAmenity(amenity.id)}
                        />
                        {amenity.name}
                      </label>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </form>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-20">
            <div className="w-10 h-10 border-[3px] border-gray-200 border-t-campus-600 rounded-full animate-spin" />
          </div>
        ) : (
          <div className="bg-white rounded-2xl border border-gray-100 overflow-hidden shadow-sm animate-slide-up">
            <div className="overflow-x-auto">
              <Table>
                <TableHeader className="bg-gray-50/50 rounded-t-2xl">
                  <TableRow className="border-b border-gray-100 hover:bg-transparent">
                    {['ID', 'Name', 'Type', 'Features', 'Capacity', 'Location', 'Availability', 'Status'].map((h) => (
                      <TableHead key={h} className="px-5 py-3.5 text-[11px] font-bold text-gray-400 uppercase tracking-wider h-auto">
                        {h}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {resources.map((r) => (
                    <TableRow key={r.id} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                      <TableCell className="px-5 py-4 text-sm font-mono text-gray-500">#{r.id}</TableCell>
                      <TableCell className="px-5 py-4 text-sm font-semibold text-campus-800">{r.name}</TableCell>
                      <TableCell className="px-5 py-4 text-sm text-gray-600">
                        <span className="inline-flex items-center px-2 py-1 rounded bg-gray-100 text-xs font-medium text-gray-600">
                          {typeLabels[r.type] || r.type}
                        </span>
                      </TableCell>
                      <TableCell className="px-5 py-4">
                        <div className="flex flex-wrap gap-1.5 max-w-[180px]">
                          {r.assetIds && r.assetIds.length > 0 && (
                            <span className="px-2 py-0.5 bg-blue-50 text-blue-600 text-[10px] font-bold rounded">
                              {r.assetIds.length} Assets
                            </span>
                          )}
                          {r.amenityIds && r.amenityIds.length > 0 && (
                            <span className="px-2 py-0.5 bg-purple-50 text-purple-600 text-[10px] font-bold rounded">
                              {r.amenityIds.length} Amenities
                            </span>
                          )}
                          {(!r.assetIds?.length && !r.amenityIds?.length) && (
                            <span className="text-[11px] text-gray-400 italic">—</span>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="px-5 py-4 text-sm text-gray-600">
                        {r.capacity ? <span className="font-semibold">{r.capacity} pax</span> : <span className="text-gray-400">—</span>}
                      </TableCell>
                      <TableCell className="px-5 py-4 text-sm text-gray-600">{r.location || '—'}</TableCell>
                      <TableCell className="px-5 py-4 text-sm text-gray-500">{r.availabilityWindows || '—'}</TableCell>
                      <TableCell className="px-5 py-4">
                        <span className={`px-2.5 py-1 text-[10px] font-bold rounded-md ${statusStyle[r.status]}`}>
                          {r.status.replace(/_/g, ' ')}
                        </span>
                      </TableCell>
                    </TableRow>
                  ))}
                  {resources.length === 0 && (
                    <TableRow className="hover:bg-transparent">
                      <TableCell colSpan={8} className="px-5 py-16 text-center">
                        <div className="flex flex-col items-center justify-center">
                          <div className="w-12 h-12 bg-gray-50 rounded-full flex items-center justify-center mb-3 border border-gray-100">
                            <svg className="w-6 h-6 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M2.25 21h19.5m-18-18v18m10.5-18v18m6-13.5V21M6.75 6.75h.75m-.75 3h.75m-.75 3h.75m3-6h.75m-.75 3h.75m-.75 3h.75M6.75 21v-3.375c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21M3 3h12m-.75 4.5H21m-3.75 3.75h.008v.008h-.008v-.008zm0 3h.008v.008h-.008v-.008zm0 3h.008v.008h-.008v-.008z" /></svg>
                          </div>
                          <p className="text-sm font-semibold text-gray-900 mb-1">No facilities found</p>
                          <p className="text-xs text-gray-500">Adjust your search filters to find resources.</p>
                        </div>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>
            {resources.length > 0 && (
              <div className="px-5 py-3 bg-gray-50/80 border-t border-gray-100 text-[11px] font-semibold text-gray-400 uppercase tracking-wider">
                Showing {resources.length} result{resources.length !== 1 ? 's' : ''}
              </div>
            )}
          </div>
        )}
      </div>
    </AppLayout>
  );
}
